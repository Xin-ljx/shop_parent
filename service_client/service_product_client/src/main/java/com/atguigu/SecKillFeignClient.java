package com.atguigu;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "shop-seckill")
public interface SecKillFeignClient {
    //1.秒杀商品列表
    @GetMapping("/seckill/queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct();
    //2.秒杀商品详情信息
    @GetMapping("/seckill/querySecKillBySkuId/{skuId}")
    public SeckillProduct querySecKillBySkuId(@PathVariable Long skuId);
    //6.返回秒杀确认页面需要的数据信息
    @GetMapping("/seckill/secKillConfirm")
    public RetVal secKillConfirm();
}
