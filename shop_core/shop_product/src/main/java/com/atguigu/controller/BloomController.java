package com.atguigu.controller;

import com.atguigu.entity.SkuInfo;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/init")
public class BloomController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private RBloomFilter rBloomFilter;

    //首先获取id,然后把获取的id放入到容器当中
    //TODO 定时任务 同步数据库与布隆过滤器
    @GetMapping("/sku/bloom")
    public String skuBloom(){
        //清空之前的布隆过滤器
        rBloomFilter.delete();
        //初始化布隆过滤器
        rBloomFilter.tryInit(10000,0.001);

        QueryWrapper<SkuInfo> wrapper = new QueryWrapper<>();
        wrapper.select("id");
        List<SkuInfo> list = skuInfoService.list(wrapper);
        for (SkuInfo skuInfo : list) {
            Long id = skuInfo.getId();
            rBloomFilter.add(id);
        }
        return "success";
    }
}
