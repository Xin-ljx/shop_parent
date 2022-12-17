package com.atguigu.service.impl;

import com.atguigu.entity.BaseBrand;
import com.atguigu.dao.BaseBrandMapper;
import com.atguigu.service.BaseBrandService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 品牌表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-10-31
 */
@Service
public class BaseBrandServiceImpl extends ServiceImpl<BaseBrandMapper, BaseBrand> implements BaseBrandService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Override
    public synchronized void setNum() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        doBusiness();
        lock.unlock();
/*        String token = UUID.randomUUID().toString();
        //从redis中获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token,3, TimeUnit.SECONDS);
        if (lock){
            //如果拿到了锁就执行业务
            doBusiness();
            //做完业务之后需要删除锁
            String luaScript="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
            //把脚本放入redisScript
            defaultRedisScript.setScriptText(luaScript);
            //设置脚本返回的数据类型
            defaultRedisScript.setResultType(Long.class);
            redisTemplate.execute(defaultRedisScript, Arrays.asList("lock"),token);
         *//*   if (token.equals(redisTemplate.opsForValue().get("lock"))){
                redisTemplate.delete("lock");
            }*//*
        }else{
            setNum();
        }*/

    }

    private void doBusiness(){
        String value = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(value)){
            //能进来说明没有初始值,将初始值设置为1
            redisTemplate.opsForValue().set("num","1");
        }else{
            //能进来说明有初始值,将初始值加一
            int num = Integer.parseInt(value);
            redisTemplate.opsForValue().set("num",String.valueOf(++num));
        }
    }
}
