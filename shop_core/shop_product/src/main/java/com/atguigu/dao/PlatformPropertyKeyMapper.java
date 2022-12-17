package com.atguigu.dao;

import com.atguigu.entity.PlatformPropertyKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 属性表 Mapper 接口
 * </p>
 *
 * @author lijiaxin
 * @since 2022-10-30
 */
public interface PlatformPropertyKeyMapper extends BaseMapper<PlatformPropertyKey> {

    List<PlatformPropertyKey> getPlatformPropertyByCategoryId(@Param("category1Id") Long category1Id,@Param("category2Id")  Long category2Id,@Param("category3Id")  Long category3Id);


    List<PlatformPropertyKey> getPlatformPropertyBySkuId(@Param("skuId") Long skuId);
}
