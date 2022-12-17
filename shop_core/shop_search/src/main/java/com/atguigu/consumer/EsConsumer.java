package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.service.SearchService;
import com.atguigu.util.MD5;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EsConsumer {
    @Autowired
    private SearchService searchService;
    @Autowired
    private RedisTemplate redisTemplate;
    //商品上架
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.ON_SALE_QUEUE),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE),
            key = {MqConst.ON_SALE_ROUTING_KEY}))
    public void onSaleToEs(Long skuId, Message message, Channel channel) throws IOException {
        //获取skuId判断是否为空
        if (skuId!=null){
            searchService.onSale(skuId);
        }
        String encryptSuffix = MD5.encrypt(skuId+"");
        Long count = redisTemplate.opsForValue().increment(RedisConst.RETRY_KEY + encryptSuffix);
        //判断重复的次数
        if (count<=RedisConst.MAX_COUNT){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else{
            //TODO 这里可以给网站管理人员发送一个短信提醒(山东鼎信)
            redisTemplate.delete(RedisConst.RETRY_KEY + encryptSuffix);
        }
    }

    //商品下架
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.OFF_SALE_QUEUE),
            exchange = @Exchange(value = MqConst.ON_OFF_SALE_EXCHANGE),
            key = {MqConst.OFF_SALE_ROUTING_KEY}))
    public void offSaleToEs(Long skuId, Message message, Channel channel) throws IOException {
        //获取skuId判断是否为空
        if (skuId!=null){
            searchService.offSale(skuId);
        }
        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
