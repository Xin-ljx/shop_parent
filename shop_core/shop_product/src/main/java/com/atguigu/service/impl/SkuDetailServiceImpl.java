package com.atguigu.service.impl;

import com.atguigu.dao.SkuSalePropertyValueMapper;
import com.atguigu.service.SkuDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkuDetailServiceImpl implements SkuDetailService {
    @Autowired
    private SkuSalePropertyValueMapper skuSalePropertyValueMapper;
    @Override
    public Map<Object, Object> getSalePropertyIdAndSkuIdMapping(Long productId) {
        Map<Object, Object> resultMap = new HashMap<>();
        List<Map> mapList =  skuSalePropertyValueMapper.getSalePropertyIdAndSkuIdMapping(productId);
        for (Map map : mapList) {
            resultMap.put(map.get("sale_property_value_id"),map.get("sku_id"));
        }
        return resultMap;
    }
}
