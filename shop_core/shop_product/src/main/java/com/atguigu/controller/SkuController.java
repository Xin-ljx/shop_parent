package com.atguigu.controller;


import com.atguigu.SearchFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.dao.ProductSalePropertyKeyMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.SkuInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 库存单元图片表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
@RestController
@RequestMapping("/product")
public class SkuController {
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private ProductImageService imageService;
    @Autowired
    private ProductSalePropertyKeyMapper salePropertyKeyMapper;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //分页显示SKU属性列表
    //http://127.0.0.1/product/querySkuInfoByPage/1/10
    @GetMapping("/querySkuInfoByPage/{pageNum}/{pageSize}")
    public RetVal querySkuInfoByPage(@PathVariable Long pageNum,
                                     @PathVariable Long pageSize){
        Page<SkuInfo> page = new Page<>(pageNum,pageSize);
        skuInfoService.page(page, null);
        return RetVal.ok(page);
    }
    //查询修改SKU属性信息
    //http://127.0.0.1/product/querySalePropertyByProductId/15
    @GetMapping("/querySalePropertyByProductId/{spuId}")
    public RetVal querySalePropertyByProductId(@PathVariable Long spuId){
        List<ProductSalePropertyKey> salePropertyKeyList=salePropertyKeyMapper.querySalePropertyByProductId(spuId);
        return RetVal.ok(salePropertyKeyList);
    }
    //查询修改SKU属性的图片信息
    //http://127.0.0.1/product/queryProductImageByProductId/15
    @GetMapping("/queryProductImageByProductId/{spuId}")
    public RetVal queryProductImageByProductId(@PathVariable Long spuId){
        QueryWrapper<ProductImage> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id",spuId);
        List<ProductImage> productImageList = imageService.list(wrapper);
        return RetVal.ok(productImageList);
    }
    //保存SKU添加的信息
    //http://127.0.0.1/product/saveSkuInfo
    @PostMapping("/saveSkuInfo")
    public RetVal saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuInfoService.saveSkuInfo(skuInfo);
        return RetVal.ok();
    }
    //下架商品信息
    //http://127.0.0.1/product/offSale/34
    @GetMapping("/offSale/{skuId}")
    public RetVal offSale(@PathVariable Long skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoService.updateById(skuInfo);
        /*searchFeignClient.offSale(skuId);*/
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.OFF_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }
    //上架商品信息
    //http://127.0.0.1/product/onSale/45
    @GetMapping("/onSale/{skuId}")
    public RetVal onSale(@PathVariable Long skuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoService.updateById(skuInfo);
        /*searchFeignClient.onSale(skuId);*/
        rabbitTemplate.convertAndSend(MqConst.ON_OFF_SALE_EXCHANGE,MqConst.ON_SALE_ROUTING_KEY,skuId);
        return RetVal.ok();
    }
}

