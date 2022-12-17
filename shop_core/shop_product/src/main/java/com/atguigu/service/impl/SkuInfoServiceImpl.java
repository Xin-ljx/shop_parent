package com.atguigu.service.impl;

import com.atguigu.cache.ShopCache;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SkuImage;
import com.atguigu.entity.SkuInfo;
import com.atguigu.entity.SkuPlatformPropertyValue;
import com.atguigu.entity.SkuSalePropertyValue;
import com.atguigu.exception.SleepUtils;
import com.atguigu.dao.SkuInfoMapper;
import com.atguigu.service.SkuImageService;
import com.atguigu.service.SkuInfoService;
import com.atguigu.service.SkuPlatformPropertyValueService;
import com.atguigu.service.SkuSalePropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 库存单元表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {
    @Autowired
    private SkuImageService imageService;
    @Autowired
    private SkuPlatformPropertyValueService platformPropertyValueService;
    @Autowired
    private SkuSalePropertyValueService skuSalePropertyValueService;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private RBloomFilter skuBloomFilter;
    @Autowired
    private RedissonClient redissonClient;
    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        //添加SKU属性信息
        baseMapper.insert(skuInfo);
        //添加SKU属性中的图片信息
        Long skuId = skuInfo.getId();
        Long productId = skuInfo.getProductId();
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuId);
            }
            imageService.saveBatch(skuImageList);
        }
        //添加skuSalePropertyValueList信息
        List<SkuSalePropertyValue> salePropertyValues = skuInfo.getSkuSalePropertyValueList();
        if (!CollectionUtils.isEmpty(salePropertyValues)){
            for (SkuSalePropertyValue salePropertyValue : salePropertyValues) {
                salePropertyValue.setSkuId(skuId);
                salePropertyValue.setProductId(productId);
            }
            skuSalePropertyValueService.saveBatch(salePropertyValues);
        }
        //添加skuPlatFromPropertyValueList
        List<SkuPlatformPropertyValue> platformPropertyValues = skuInfo.getSkuPlatformPropertyValueList();
        if (!CollectionUtils.isEmpty(platformPropertyValues)){
            for (SkuPlatformPropertyValue platformPropertyValue : platformPropertyValues) {
                platformPropertyValue.setSkuId(skuId);
            }
            platformPropertyValueService.saveBatch(platformPropertyValues);
        }
    }
    //封装图片列表到基本信息列表
    @Override
    @ShopCache(value = "skuInfo")
    public SkuInfo getSkuInfo(Long skuId) {
//        SkuInfo skuInfo = getSkuInfoFromRedisson(skuId);

        //SkuInfo skuInfo = getSkuInfoFromDB(skuId);
        SkuInfo skuInfo = getSkuInfoFromRedis(skuId);
        return skuInfo;
    }

    //使用布隆过滤器
    private SkuInfo getSkuInfoFromRedisson(Long skuId){
        String lockKey="lock-"+skuId;
        String key = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo redisValue = (SkuInfo) redisTemplate.opsForValue().get(key);
        RLock lock = redissonClient.getLock(lockKey);
        //判断redis是否为空,若redis为空则从数据库获取数据
        if (redisValue == null){
            try {
                lock.lock();
                boolean flag = skuBloomFilter.contains(skuId);
                if (flag){
                    //能进来说明包含
                    SkuInfo skuInfo = getSkuInfoFromDB(skuId);
                    redisTemplate.opsForValue().set(key,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    return skuInfo;
                }
            } finally {
                //释放锁资源
                lock.unlock();
            }
        }
        return redisValue;
    }


    private SkuInfo getSkuInfoRedisson(Long skuId){
        String lockKey = "lock-" + skuId;
        String key = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo redisValue = (SkuInfo) redisTemplate.opsForValue().get(key);
        return redisValue;
    }

    ThreadLocal<String> threadLocal = new ThreadLocal();
    private synchronized SkuInfo getSkuInfoFromRedisWithThreadLocal(Long skuId) {
        String lockKey = "lock-" + skuId;
        String key = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo redisValue = (SkuInfo) redisTemplate.opsForValue().get(key);
        if (redisValue == null) {
            //这里可能有很多代码需要去操作
            String token = threadLocal.get();
            boolean local = false;
            if (token != null) {
                //已经拿到过锁了 不要再去拿锁了
                local = true;
            } else {
                token = UUID.randomUUID().toString();
                //利用redis的setnx命令
                local = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
            }
            if (local) {
                //拿到锁就直接去执行业务
                SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
                //把数据放入缓存
                redisTemplate.opsForValue().set(key, skuInfoFromDB, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                //能进来说明拿到锁了,拿到就执行业务,使用lua脚本
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
                //把脚本放入到RedisScript中
                defaultRedisScript.setScriptText(luaScript);
                //设置脚本返回数据的数据类型
                defaultRedisScript.setResultType(Long.class);
                redisTemplate.execute(defaultRedisScript, Arrays.asList("lock"), token);
                //业务执行完需要清空map,否则老年代的内存会一直增多
                threadLocal.remove();
                return skuInfoFromDB;
            } else {
                //再查一次缓存,没必要没抢到锁就全部递归抢锁,浪费资源,缓存查到了就直接返回
                //自旋,目的是可以拿到锁资源
                while (true) {
                    SleepUtils.sleep(50);
                    Boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
                    if (retryAccquireLock) {
                        threadLocal.set(token);
                        break;
                    }
                }
                return getSkuInfoFromRedisWithThreadLocal(skuId);
            }
        }
        return redisValue;
    }
/*        //判断redis是否为空,若redis为空则从数据库获取数据
        if (redisValue == null){
            SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
            redisTemplate.opsForValue().set(key,skuInfoFromDB,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            return skuInfoFromDB;
        }*/


    //从数据库获取数据
    private SkuInfo getSkuInfoFromDB(Long skuId){
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        if (skuInfo!=null){
            QueryWrapper<SkuImage> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = imageService.list(wrapper);
            skuInfo.setSkuImageList(skuImageList);
        }
        return skuInfo;
    }
    //从redis获取数据
    private SkuInfo getSkuInfoFromRedis(Long skuId){
        String key = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
        SkuInfo redisValue = (SkuInfo) redisTemplate.opsForValue().get(key);
        //判断redis是否为空,若redis为空则从数据库获取数据
        if (redisValue == null){
            SkuInfo skuInfoFromDB = getSkuInfoFromDB(skuId);
            redisTemplate.opsForValue().set(key,skuInfoFromDB,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
            return skuInfoFromDB;
        }
        return redisValue;
    }

}
