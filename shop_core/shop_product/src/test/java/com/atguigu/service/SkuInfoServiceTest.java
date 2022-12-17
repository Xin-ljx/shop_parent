package com.atguigu.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class SkuInfoServiceTest {
@Autowired
private SkuInfoService skuInfoService;
    @Test
    void getSkuInfo() {
        System.out.println(skuInfoService.getSkuInfo(24L));
    }
}