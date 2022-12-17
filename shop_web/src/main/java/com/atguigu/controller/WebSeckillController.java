package com.atguigu.controller;

import com.atguigu.SecKillFeignClient;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.result.RetVal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@Controller
public class WebSeckillController {
    @Autowired
    private SecKillFeignClient secKillFeignClient;
    //1.秒杀列表 按理说我们可以在这里直接从redis中查询秒杀列表信息 但是这里没有redis依赖
    @GetMapping("seckill-index.html")
    public String seckillIndex(Model model) {
        List<SeckillProduct> seckillProductList = secKillFeignClient.queryAllSecKillProduct();
        model.addAttribute("list",seckillProductList);
        return "seckill/index";
    }
    //2.秒杀详情页面
    @GetMapping("seckill-detail/{skuId}.html")
    public String seckillDetail(@PathVariable Long skuId, Model model) {
        SeckillProduct seckillProduct = secKillFeignClient.querySecKillBySkuId(skuId);
        model.addAttribute("item",seckillProduct);
        return "seckill/detail";
    }

    //3.秒杀商品详情
    @GetMapping("seckill-queue.html")
    public String seckillQueue(Long skuId,String seckillCode, Model model){
        model.addAttribute("skuId",skuId);
        model.addAttribute("seckillCode",seckillCode);
        return "seckill/queue";
    }
    //4.秒杀确认页面
    @GetMapping("seckill-confirm.html")
    public String seckillConfirm(Model model) {
        RetVal retVal = secKillFeignClient.secKillConfirm();
        if(retVal.isOk()){
            Map<String, Object> retMap=(Map<String, Object>)retVal.getData();
            model.addAllAttributes(retMap);
        }else{
            model.addAttribute("message",retVal.getMessage());
        }
        return "seckill/confirm";
    }
}
