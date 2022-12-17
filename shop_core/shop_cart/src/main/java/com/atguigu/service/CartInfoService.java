package com.atguigu.service;

import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-14
 */
public interface CartInfoService extends IService<CartInfo> {

    RetVal addCart(String oneOfUserId, Long skuId, Integer skuNum);

    RetVal getCartList(String userId, String userTempId);


    void checkCart(String oneOfUserId, Long skuId, Integer isChecked);

    void deleteCart(Long skuId, String oneOfUserId);

    List<CartInfo> getSelectedCartInfo(String userId);
}
