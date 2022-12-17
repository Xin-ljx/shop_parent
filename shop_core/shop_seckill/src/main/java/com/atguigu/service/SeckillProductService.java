package com.atguigu.service;

import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-23
 */
public interface SeckillProductService extends IService<SeckillProduct> {

    SeckillProduct querySecKillBySkuId(Long skuId);

    List<SeckillProduct> queryAllSecKillProduct();


    void prepareSecKill(UserSeckillSkuInfo userSeckillSkuInfo);

    RetVal hasQualified(Long skuId, String userId);

    RetVal secKillConfirm(String userId);
}
