package com.atguigu.utils;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FastdfsUtils implements InitializingBean {
    @Value("${filename.url}")
    private String url;

    public static String URL;
    @Override
    public void afterPropertiesSet() throws Exception {
        this.URL = this.url;
    }
}
