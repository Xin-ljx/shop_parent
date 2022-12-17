package com.atguigu.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.ProductFeignClient;
import com.atguigu.SearchFeignClient;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
public class WebSkuDetailController {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor myThreadPool;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @RequestMapping("{skuId}.html")
    public String getSkuDetail(@PathVariable Long skuId, Model model){
        //runAsync无参无返回值
        CompletableFuture<Void> skuPriceFuture = CompletableFuture.runAsync(() -> {
            //获取实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            model.addAttribute("price", skuPrice);
        },myThreadPool);
        //supply无参有返回值
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //根据skuId查询商品的基本信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            model.addAttribute("skuInfo", skuInfo);
            return skuInfo;
        },myThreadPool);
        //accept有参无返回值
        CompletableFuture<Void> categoryView = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //根据三级分类id获取商品的分类信息
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView1 = productFeignClient.getCategoryView(category3Id);
            model.addAttribute("categoryView", categoryView1);
        },myThreadPool);
        CompletableFuture<Void> salePropertyIdAndSkuIdMapping = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取销售属性组合与skuId的对应关系
            Map<Object, Object> salePropertyIdAndSkuIdMapping1 = productFeignClient.getSalePropertyIdAndSkuIdMapping(skuInfo.getProductId());
            model.addAttribute("salePropertyValueIdJson", JSON.toJSONString(salePropertyIdAndSkuIdMapping1));
        },myThreadPool);
        CompletableFuture<Void> spuSalePropertyList = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取该sku对应的销售属性(一份)和所有的销售属性(全份)
            List<ProductSalePropertyKey> spuSalePropertyList1 = productFeignClient.getSpuSalePropertyAndSelected(skuInfo.getProductId(), skuId);
            model.addAttribute("spuSalePropertyList", spuSalePropertyList1);
        },myThreadPool);
        CompletableFuture.runAsync(() -> {
            searchFeignClient.incrHotScore(skuId);
        }, myThreadPool);
        CompletableFuture.allOf(skuPriceFuture,
                skuInfoCompletableFuture,
                categoryView,
                salePropertyIdAndSkuIdMapping,
                spuSalePropertyList).join();
        return "detail/index";
    }
}
