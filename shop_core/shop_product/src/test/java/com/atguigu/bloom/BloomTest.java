package com.atguigu.bloom;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BloomTest {
    @Autowired
    private RBloomFilter rBloomFilter;
    @Test
    public void skuTest(){
        boolean flag24 = rBloomFilter.contains(24L);
        System.out.println(flag24);
        boolean flag55 = rBloomFilter.contains(55L);
        System.out.println(flag55);
    }
}
