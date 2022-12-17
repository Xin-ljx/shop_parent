package com.atguigu.fallback;

import com.atguigu.ProductFeignClient;
import com.atguigu.entity.*;
import com.atguigu.result.RetVal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProductDegradeFeignClient implements ProductFeignClient {
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    @Override
    public Map<Object, Object> getSalePropertyIdAndSkuIdMapping(Long productId) {
        return null;
    }

    @Override
    public List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(Long productId, Long skuId) {
        return null;
    }

    @Override
    public RetVal gateIndexCategory() {
        return null;
    }

    @Override
    public BaseBrand getBrandById(Long brandId) {
        return null;
    }

    @Override
    public List<PlatformPropertyKey> getPlatformPropertyBySkuId(Long skuId) {
        return null;
    }


}
