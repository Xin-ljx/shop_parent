package com.atguigu.consumer;

import com.atguigu.constant.MqConst;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;


@Component
public class PaymentConsumer {
    @Autowired
    private PaymentInfoService paymentInfoService;
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.CLOSE_PAYMENT_EXCHANGE),
            value = @Queue(value = MqConst.CLOSE_PAYMENT_QUEUE),
            key = {MqConst.CLOSE_PAYMENT_ROUTE_KEY}))
    public void updateOrderAfterPaySuccess(String outTradeNo, Message message, Channel channel) throws IOException {
        if(!StringUtils.isEmpty(outTradeNo)){
            //修改支付表中的支付状态,将支付状态改为关闭
            PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(outTradeNo);
            if (paymentInfo!=null){
                paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
                paymentInfoService.updateById(paymentInfo);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
