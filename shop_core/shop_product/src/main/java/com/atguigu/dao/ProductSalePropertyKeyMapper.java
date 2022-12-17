package com.atguigu.dao;

import com.atguigu.entity.ProductSalePropertyKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * spu销售属性 Mapper 接口
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
public interface ProductSalePropertyKeyMapper extends BaseMapper<ProductSalePropertyKey> {

    List<ProductSalePropertyKey> querySalePropertyByProductId(@Param("spuId")Long spuId);

    List<ProductSalePropertyKey> getSpuSalePropertyAndSelected(@Param("productId") Long productId, @Param("skuId") Long skuId);
}
