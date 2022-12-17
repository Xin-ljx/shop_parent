package com.atguigu.dao;

import com.atguigu.entity.TOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-20
 */
public interface TOrder1Mapper extends BaseMapper<TOrder> {

    /*List<TOrder> queryOrderAndDetail(@Param("userId") Long userId, @Param("orderId") Long orderId);*/

    List<TOrder> queryOrderAndDetail(@Param("userId") Long userId);
}
