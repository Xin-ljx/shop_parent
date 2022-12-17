package com.atguigu.service.impl;

import com.atguigu.UserFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.dao.SeckillProductMapper;
import com.atguigu.entity.*;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.MD5;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-23
 */
@Service
public class SeckillProductServiceImpl extends ServiceImpl<SeckillProductMapper, SeckillProduct> implements SeckillProductService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserFeignClient userFeignClient;
    //
    public Map<Long,SeckillProduct> firstLevelCache = new ConcurrentHashMap<>();
    @Override
    public SeckillProduct querySecKillBySkuId(Long skuId) {
        SeckillProduct seckillProduct = firstLevelCache.get(skuId);
        if (seckillProduct == null){
            seckillProduct = (SeckillProduct) redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).get(skuId.toString());
        }
        return seckillProduct;
    }

    @Override
    public List<SeckillProduct> queryAllSecKillProduct() {
        //第一次要从存存汇总查
        Collection<SeckillProduct> collection = firstLevelCache.values();
        if (collection.size()>0){
            //能进来说明命中了一级缓存
            return collection.stream().sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
        }
        //将数据添加到一级缓存中
        List<SeckillProduct> seckillProductList = redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).values();
        List<SeckillProduct> collectSecKillList = seckillProductList.stream().map(seckillProduct -> {
            firstLevelCache.put(seckillProduct.getSkuId(),seckillProduct);
            return seckillProduct;
        }).sorted(Comparator.comparing(SeckillProduct::getStartTime)).collect(Collectors.toList());
        return collectSecKillList;
    }
    //预下单
    @Override
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo) {
        Long skuId = userSeckillSkuInfo.getSkuId();
        String userId = userSeckillSkuInfo.getUserId();
        String status = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
        //校验秒杀状态位
        if (RedisConst.CAN_NOT_SECKILL.equals(status)){
            return;
        }
        //校验是否下过单
        Boolean flag=redisTemplate.opsForValue().setIfAbsent(RedisConst.PREPARE_SECKILL_USERID_SKUID+":"+userId+":"+skuId,skuId.toString(),RedisConst.PREPARE_SECKILL_LOCK_TIME, TimeUnit.SECONDS);
        if(!flag){
            return;
        }
        //校验商品库存
        String redisStockSkuId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(redisStockSkuId)){
            //如果库存为空的话需要通知其他服务修改状态位
            redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_NOT_SECKILL);
        }
        //生成临时订单数据,存入redis
        PrepareSeckillOrder prepareSeckillOrder = new PrepareSeckillOrder();
        prepareSeckillOrder.setUserId(userId);
        prepareSeckillOrder.setBuyNum(1);
        SeckillProduct seckillProduct = querySecKillBySkuId(skuId);
        prepareSeckillOrder.setSeckillProduct(seckillProduct);
        prepareSeckillOrder.setPrepareOrderCode(MD5.encrypt(userId+skuId));
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).put(userId,prepareSeckillOrder);
        //更新库存
        updateSecKillStockCount(skuId);
    }
    //判断是否有秒杀资格
    @Override
    public RetVal hasQualified(Long skuId, String userId) {
        //如果预购订单里边有用户和skuId的对应关系的话就证明有秒杀资格
        Boolean isExist = redisTemplate.hasKey(RedisConst.PREPARE_SECKILL_USERID_SKUID + ":" + userId + ":" + skuId);
        if (isExist){
            //获取预购单的信息
            PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
            if (prepareSeckillOrder!=null){
                //返回具备抢购资格的预购订单
                return RetVal.build(prepareSeckillOrder, RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
            }
        }
        Integer orderId = (Integer) redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).get(userId);
        if (orderId!=null){
            //能进来代表已经抢到了商品,此页面显示抢购成功
            return RetVal.build(null,RetValCodeEnum.PREPARE_SECKILL_SUCCESS);
        }
        //如果预购单中没有对应关系
        return RetVal.build(null,RetValCodeEnum.SECKILL_RUN);
    }
    //返回秒杀确认页面需要的数据信息
    @Override
    public RetVal secKillConfirm(String userId) {
        //a.收货人的地址信息
        List<UserAddress> userAddressList = userFeignClient.getAddressByUserId(userId);
        //b.从redis里面去获得预购订单信息
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder==null){
            return RetVal.fail().message("非法请求");
        }
        //c.把预购单信息转换为订单详情信息
        SeckillProduct seckillProduct = prepareSeckillOrder.getSeckillProduct();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillProduct.getSkuId());
        orderDetail.setSkuName(seckillProduct.getSkuName());
        orderDetail.setSkuNum(prepareSeckillOrder.getBuyNum()+"");
        orderDetail.setImgUrl(seckillProduct.getSkuDefaultImg());
        orderDetail.setOrderPrice(seckillProduct.getCostPrice());
        List<OrderDetail> orderDetailList = new ArrayList<>();
        orderDetailList.add(orderDetail);
        //d.把这些信息放到一个map当中
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("userAddressList",userAddressList);
        retMap.put("orderDetailList",orderDetailList);
        retMap.put("totalMoney",seckillProduct.getCostPrice());
        return RetVal.ok(retMap);
    }

    private void updateSecKillStockCount(Long skuId) {
        Long leftStock = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //锁定库存=总数量-剩余库存
        SeckillProduct redisSeckillProduct = querySecKillBySkuId(skuId);
        Integer totalStock = redisSeckillProduct.getNum();
        Integer lockStock=totalStock-Integer.parseInt(leftStock+"");
        redisSeckillProduct.setStockCount(lockStock);
        //更新到redis里面的商品信息里面 目的是给消费者看 才能知道当前的一个进度
        redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId.toString(),redisSeckillProduct);
        //更新的数据库,每每有两条记录就更新一次数据库
        if (leftStock % 2 == 0){
            baseMapper.updateById(redisSeckillProduct);
        }
    }

}
