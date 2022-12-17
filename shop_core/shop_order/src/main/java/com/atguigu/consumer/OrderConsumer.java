package com.atguigu.consumer;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.PaymentFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.MD5;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;


@Component
public class OrderConsumer {
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private PaymentFeignClient paymentFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @RabbitListener(queues = MqConst.CANCEL_ORDER_QUEUE)
    public void cancelOrder(Long orderId, Message message, Channel channel) throws IOException {
        //判断orderId是否为空
        if (orderId != null){
            OrderInfo orderInfo = orderInfoService.getById(orderId);
            //修改订单的状态
            orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
            orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
            orderInfoService.updateById(orderInfo);
            //如果支付宝也有交易记录需要把支付宝中的交易记录也关闭
            rabbitTemplate.convertAndSend(MqConst.CLOSE_PAYMENT_EXCHANGE,MqConst.CLOSE_PAYMENT_ROUTE_KEY,orderInfo.getOutTradeNo());
            Boolean flag = paymentFeignClient.queryAlipayTrade(orderId);
            if (flag){
                paymentFeignClient.closeAlipayTrade(orderId);
            }
        }
        //multiple属性确认是否持久化,如果出现了异常会出现一直请求重复消费的情况,redis计数解决

        String encryptSuffix = MD5.encrypt(orderId+"");
        Long count = redisTemplate.opsForValue().increment(RedisConst.RETRY_KEY + encryptSuffix);
        //判断重复的次数
        if (count<=RedisConst.MAX_COUNT){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }else{
            //TODO 这里可以给网站管理人员发送一个短信提醒(山东鼎信)
            redisTemplate.delete(RedisConst.RETRY_KEY + encryptSuffix);
        }
    }
    //支付成功后
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.PAY_ORDER_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.PAY_ORDER_EXCHANGE,durable = "false"),
            key = {MqConst.PAY_ORDER_ROUTE_KEY}))
    public void updateOrderAfterPaySuccess(Long orderId,Message message,Channel channel) throws IOException {
        if (orderId!=null){
            OrderInfo orderInfo = orderInfoService.getOrderInfoAndDetail(orderId);
            //判断当订单状态为未支付时候才修改状态
            if (orderInfo!=null && OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())){
                //修改订单状态为已支付
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.PAID);
                //给库存系统发送一个减库存消息
                orderInfoService.sendMsgToWareManage(orderInfo);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
    //3.仓库系统减库存成功之后的代码
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.SUCCESS_DECREASE_STOCK_QUEUE,durable = "false"),
            exchange = @Exchange(value = MqConst.SUCCESS_DECREASE_STOCK_EXCHANGE,durable = "false"),
            key = {MqConst.SUCCESS_DECREASE_STOCK_ROUTE_KEY}))
    public void updateOrderAfterDecreaseStock(String jsonData,Channel channel,Message message) throws IOException {
        if (StringUtils.isEmpty(jsonData)){
            //把json转换为map
            Map map = JSONObject.parseObject(jsonData, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            //如果仓库系统那边已经减库存成功 把订单状态改为等待发货
            OrderInfo orderInfo = orderInfoService.getOrderInfoAndDetail(Long.valueOf(orderId));
            if ("DEDUCTED".equals(status)){
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.NOTIFIED_WARE);
            }else {
                orderInfoService.updateOrderStatus(orderInfo,ProcessStatus.STOCK_EXCEPTION);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
