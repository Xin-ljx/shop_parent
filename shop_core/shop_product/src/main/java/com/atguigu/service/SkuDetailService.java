package com.atguigu.service;


import java.util.Map;

public interface SkuDetailService {
    Map<Object, Object> getSalePropertyIdAndSkuIdMapping(Long productId);
}
