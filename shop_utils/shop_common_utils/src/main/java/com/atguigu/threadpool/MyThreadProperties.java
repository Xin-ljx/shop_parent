package com.atguigu.threadpool;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "thread.pool")
public class MyThreadProperties {

    private Integer corePoolSize = 16;

    private Integer maxiumPoolSize = 32;

    private Long keepAliveTime = 50L;

    private Integer queueLength = 100;
}
