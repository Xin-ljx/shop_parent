package com.atguigu.service.impl;

import com.atguigu.entity.ProductImage;
import com.atguigu.entity.ProductSalePropertyKey;
import com.atguigu.entity.ProductSalePropertyValue;
import com.atguigu.entity.ProductSpu;
import com.atguigu.dao.ProductSpuMapper;
import com.atguigu.service.ProductImageService;
import com.atguigu.service.ProductSalePropertyKeyService;
import com.atguigu.service.ProductSalePropertyValueService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
@Service
public class ProductSpuServiceImpl extends ServiceImpl<ProductSpuMapper, ProductSpu> implements ProductSpuService {
    @Autowired
    private ProductImageService imageService;
    @Autowired
    private ProductSalePropertyKeyService salePropertyKeyService;
    @Autowired
    private ProductSalePropertyValueService salePropertyValueService;

    @Override
    @Transactional
    public void saveProductSpu(ProductSpu productSpu) {
        //保存spu的基本信息
        baseMapper.insert(productSpu);
        //保存spu的图片信息
        Long spuId = productSpu.getId();
        List<ProductImage> productImageList = productSpu.getProductImageList();
        if (!CollectionUtils.isEmpty(productImageList)) {
            for (ProductImage productImage : productImageList) {
                productImage.setProductId(spuId);
            }
            imageService.saveBatch(productImageList);
        }
        //保存spu的销售属性key信息
        List<ProductSalePropertyKey> salePropertyKeyList = productSpu.getSalePropertyKeyList();
        if (!CollectionUtils.isEmpty(salePropertyKeyList)) {
            for (ProductSalePropertyKey productSalePropertyKey : salePropertyKeyList) {
                //传入的id确定操作哪个
                productSalePropertyKey.setProductId(spuId);
                //保存spu的销售属性的value信息
                List<ProductSalePropertyValue> salePropertyValueList = productSalePropertyKey.getSalePropertyValueList();
                if (!CollectionUtils.isEmpty(salePropertyValueList)) {
                    for (ProductSalePropertyValue productSalePropertyValue : salePropertyValueList) {
                        productSalePropertyKey.setSalePropertyKeyId(spuId);
                        //设置销售属性key
                        productSalePropertyValue.setSalePropertyKeyName(productSalePropertyKey.getSalePropertyKeyName());
                    }
                    salePropertyValueService.saveBatch(salePropertyValueList);
                }
            }
            salePropertyKeyService.saveBatch(salePropertyKeyList);
        }
    }
}