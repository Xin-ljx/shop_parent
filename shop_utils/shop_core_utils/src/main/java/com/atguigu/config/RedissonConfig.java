package com.atguigu.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        //创建Redisson的配置对象
        Config config = new Config();
        //在配置对象中填写地址
        config.useSingleServer().setAddress("redis://192.168.147.128:6389");
        //根据配置文件创建Redisson客户端
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
