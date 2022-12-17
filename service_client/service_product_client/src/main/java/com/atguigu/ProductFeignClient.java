package com.atguigu;

import com.atguigu.entity.*;
import com.atguigu.fallback.ProductDegradeFeignClient;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "shop-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {
    @GetMapping("/sku/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);
    //根据三级分类id获取商品的分类信息
    @GetMapping("/sku/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);
    //获取实时价格
    @GetMapping("/sku/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);
    //获取销售属性组合与skuId的对应关系
    @GetMapping("/sku/getSalePropertyIdAndSkuIdMapping/{productId}")
    public Map<Object,Object> getSalePropertyIdAndSkuIdMapping(@PathVariable Long productId);
    //获取该sku对应的销售属性(一份)和所有的销售属性(全份)
    @GetMapping("/sku/getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId,
                                                                      @PathVariable Long skuId);
    @GetMapping("/product/gateIndexCategory")
    public RetVal gateIndexCategory();

    @GetMapping("/product/brand/getBrandById/{brandId}")
    public BaseBrand getBrandById(@PathVariable Long brandId);

    //根据skuId获取平台属性信息
    @GetMapping("/product/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId);
}
