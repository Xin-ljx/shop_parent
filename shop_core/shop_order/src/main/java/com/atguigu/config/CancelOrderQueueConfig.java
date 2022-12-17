package com.atguigu.config;

import com.atguigu.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class CancelOrderQueueConfig {
    @Bean
    public Queue cancelOrderQueue(){
        return new Queue(MqConst.CANCEL_ORDER_QUEUE,false);
    }

    //延迟插件,延迟交换机,自定义交换机
    @Bean
    public CustomExchange cancelExchange(){
        HashMap<String, Object> arguments = new HashMap<>();
        //常规交换机类型
        arguments.put("x-delayed-type","direct");
        return new CustomExchange(MqConst.CANCEL_ORDER_EXCHANGE,"x-delayed-message",false,true,arguments);
    }

    @Bean
    public Binding bindingDelayedQueue(@Qualifier("cancelOrderQueue") Queue cancelOrderQueue,
                                 @Qualifier("cancelExchange") CustomExchange cancelExchange){
        return BindingBuilder.bind(cancelOrderQueue).to(cancelExchange).with(MqConst.CANCEL_ORDER_ROUTE_KEY).noargs();
    }
}
