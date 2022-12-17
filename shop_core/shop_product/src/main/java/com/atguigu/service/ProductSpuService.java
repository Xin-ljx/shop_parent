package com.atguigu.service;

import com.atguigu.entity.ProductSpu;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
public interface ProductSpuService extends IService<ProductSpu> {

    void saveProductSpu(ProductSpu productSpu);
}
