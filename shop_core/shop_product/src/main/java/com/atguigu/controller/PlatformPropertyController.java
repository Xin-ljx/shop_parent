package com.atguigu.controller;


import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.dao.PlatformPropertyKeyMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 属性表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-10-30
 */
@RestController
@RequestMapping("/product")
public class PlatformPropertyController {

    @Autowired
    private PlatformPropertyKeyService propertyKeyService;

    @Autowired
    private PlatformPropertyValueService propertyValueService;

    @Autowired
    private PlatformPropertyKeyMapper propertyKeyMapper;
    //获取平台属性列表
    //http://127.0.0.1:8000/product/getPlatformPropertyByCategoryId/1/0/0
    @GetMapping("/getPlatformPropertyByCategoryId/{category1Id}/{category2Id}/{category3Id}")
    public RetVal getPlatformPropertyByCategoryId(@PathVariable Long category1Id,
                                                  @PathVariable Long category2Id,
                                                  @PathVariable Long category3Id){
        List<PlatformPropertyKey> propertyKeyList = propertyKeyService.getPlatformPropertyByCategoryId(category1Id,category2Id,category3Id);
        return RetVal.ok(propertyKeyList);
    }
    //修改平台属性
    //http://127.0.0.1:8000/product/getPropertyValueByPropertyKeyId/4
    @GetMapping("/getPropertyValueByPropertyKeyId/{propertyKeyId}")
    public RetVal getPropertyValueByPropertyKeyId(@PathVariable Long propertyKeyId){
        QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
        wrapper.eq("property_key_id",propertyKeyId);
        List<PlatformPropertyValue> propertyValueList = propertyValueService.list(wrapper);
        return RetVal.ok(propertyValueList);
    }
    //添加保存平台属性
    //http://127.0.0.1:8000/product/savePlatformProperty
    @PostMapping("/savePlatformProperty")
    public RetVal savePlatformProperty(@RequestBody PlatformPropertyKey platformPropertyKey){
        propertyKeyService.savePlatformProperty(platformPropertyKey);
        return RetVal.ok();
    }
    //根据skuId获取平台属性信息
    @GetMapping("/getPlatformPropertyBySkuId/{skuId}")
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(@PathVariable Long skuId){
        return propertyKeyMapper.getPlatformPropertyBySkuId(skuId);
    }
}

