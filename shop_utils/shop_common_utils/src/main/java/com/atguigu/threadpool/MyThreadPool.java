package com.atguigu.threadpool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(value = MyThreadProperties.class)
public class MyThreadPool {
    @Autowired
    private MyThreadProperties myThreadProperties;

    @Bean
    public ThreadPoolExecutor myPoolExecutor() {

        ThreadPoolExecutor myThreadExecutor = new ThreadPoolExecutor(myThreadProperties.getCorePoolSize(),
                myThreadProperties.getMaxiumPoolSize(),
                myThreadProperties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                //Array会造成空间碎片的出现所以此处使用Link
                new LinkedBlockingQueue<>(myThreadProperties.getCorePoolSize()),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        return myThreadExecutor;
    }
}
