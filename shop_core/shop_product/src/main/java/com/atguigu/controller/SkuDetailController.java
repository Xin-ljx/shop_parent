package com.atguigu.controller;

import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.dao.ProductSalePropertyKeyMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.SkuDetailService;
import com.atguigu.service.SkuInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sku")
public class SkuDetailController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private BaseCategoryViewService categoryViewService;
    @Autowired
    private SkuDetailService skuDetailService;
    @Autowired
    private ProductSalePropertyKeyMapper salePropertyKeyMapper;
    @Value("8000")
    private Integer serverPort;
    //根据skuId查询商品的基本信息
    @GetMapping("/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        SkuInfo skuInfo = skuInfoService.getSkuInfo(skuId);
        return skuInfo;
    }
    //根据三级分类id获取商品的分类信息
    @GetMapping("/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return categoryViewService.getById(category3Id);
    }
    //获取实时价格
    @GetMapping("/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        SkuInfo skuInfo = skuInfoService.getById(skuId);
        return skuInfo.getPrice();
    }
    //获取销售属性组合与skuId的对应关系
    @GetMapping("/getSalePropertyIdAndSkuIdMapping/{productId}")
    public Map<Object,Object> getSalePropertyIdAndSkuIdMapping(@PathVariable Long productId){
        Map<Object, Object> salePropertyIdAndSkuIdMapping = skuDetailService.getSalePropertyIdAndSkuIdMapping(productId);
        return salePropertyIdAndSkuIdMapping;
    }
    //获取该sku对应的销售属性(一份)和所有的销售属性(全份)
    @GetMapping("/getSpuSalePropertyAndSelected/{productId}/{skuId}")
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@PathVariable Long productId,
                                                                      @PathVariable Long skuId){
        return salePropertyKeyMapper.getSpuSalePropertyAndSelected(productId, skuId);
    }
}
