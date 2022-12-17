package com.atguigu.redis_consumer;

import com.atguigu.constant.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelConfig {
    @Bean
    MessageListenerAdapter listenerAdapter(SecKillMsgReciver secKillMsgReciver){
        return new MessageListenerAdapter(secKillMsgReciver,"receiveChannelMessage");
    }


    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory factory, MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        //订阅哪一个channel
        container.addMessageListener(listenerAdapter,new PatternTopic(RedisConst.PREPARE_PUB_SUB_SECKILL));
        return container;
    }
}
