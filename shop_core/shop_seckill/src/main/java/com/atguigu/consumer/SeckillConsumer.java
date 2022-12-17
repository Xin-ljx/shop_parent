package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.service.SeckillProductService;
import com.atguigu.utils.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

@Component
public class SeckillConsumer {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.SCAN_SECKILL_EXCHANGE,durable = "false"),
            value = @Queue(value = MqConst.SCAN_SECKILL_QUEUE,durable = "false"),
            key = {MqConst.SCAN_SECKILL_ROUTE_KEY}))
    public void scanSeckillProductToRedis(){
        QueryWrapper<SeckillProduct> wrapper = new QueryWrapper<>();
        //扫描已经通过审核的秒杀商品
        wrapper.eq("status",1);
        //秒杀商品的数量要大于零
        wrapper.gt("num",0);
        //取出当天符合范围内的商品
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillProduct> productList = seckillProductService.list(wrapper);
        if(!CollectionUtils.isEmpty(productList)){
            for (SeckillProduct seckillProduct : productList) {
                String skuId = seckillProduct.getSkuId().toString();
                redisTemplate.boundHashOps(RedisConst.SECKILL_PRODUCT).put(skuId,seckillProduct);
                //把秒杀商品的数量放入的redis中
                for (Integer i = 0; i < seckillProduct.getNum(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+skuId).leftPush(skuId);
                }
                //通知其他节点改变秒杀状态位(redis的发布订阅模式)
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_SECKILL);
            }
        }
    }

    //秒杀出的rabbit
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.PREPARE_SECKILL_EXCHANGE,durable = "false"),
            value = @Queue(value = MqConst.PREPARE_SECKILL_QUEUE,durable = "false"),
            key = {MqConst.PREPARE_SECKILL_ROUTE_KEY}))
    public void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo){
        if (userSeckillSkuInfo!=null){
            //开始处理下单
            seckillProductService.prepareSecKill(userSeckillSkuInfo);
        }
    }

}
