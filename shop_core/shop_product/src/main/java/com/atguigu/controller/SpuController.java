package com.atguigu.controller;


import com.atguigu.entity.BaseSaleProperty;
import com.atguigu.entity.ProductSpu;
import com.atguigu.result.RetVal;
import com.atguigu.service.BaseSalePropertyService;
import com.atguigu.service.ProductSpuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-01
 */
@RestController
@RequestMapping("/product")
public class SpuController {
    @Autowired
    private ProductSpuService productSpuService;
    @Autowired
    private BaseSalePropertyService salePropertyService;
    //SPU分页查询
    //http://127.0.0.1/product/queryProductSpuByPage/1/10/1
    @GetMapping("/queryProductSpuByPage/{pageNum}/{pageSize}/{category3Id}")
    public RetVal queryProductSpuByPage(@PathVariable Long pageNum,
                                        @PathVariable Long pageSize,
                                        @PathVariable Long category3Id){
        Page<ProductSpu> page = new Page<>(pageNum,pageSize);
        QueryWrapper<ProductSpu> wrapper = new QueryWrapper<>();
        wrapper.eq("category3_id",category3Id);
        productSpuService.page(page, wrapper);
        return RetVal.ok(page);
    }

    //添加SPU
    //查询全部信息
    //http://127.0.0.1/product/queryAllSaleProperty
    @GetMapping("/queryAllSaleProperty")
    public RetVal queryAllSaleProperty(){
        List<BaseSaleProperty> salePropertyList = salePropertyService.list(null);
        return RetVal.ok(salePropertyList);
    }
    //保存添加的信息
    //http://127.0.0.1/product/saveProductSpu
    @PostMapping("/saveProductSpu")
    public RetVal saveProductSpu(@RequestBody ProductSpu productSpu){
        productSpuService.saveProductSpu(productSpu);
        return RetVal.ok();
    }

}

