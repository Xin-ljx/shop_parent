package com.atguigu.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//这个注解只能放到哪个地方
@Target(ElementType.METHOD)
//该注解的一个生命周期
@Retention(RetentionPolicy.RUNTIME)
public @interface ShopCache {
    //定义一个属性value
    String value() default "cache";
    //是否开启布隆过滤器
    boolean enableBloom() default false;
}
