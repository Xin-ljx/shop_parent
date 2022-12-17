package com.atguigu.controller;

import com.atguigu.CartFeignClient;
import com.atguigu.ProductFeignClient;
import com.atguigu.entity.SkuInfo;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class WebCartController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;


    @RequestMapping("/addCart.html")
    public String addCart(@RequestParam Long skuId, @RequestParam Integer skuNum, HttpServletRequest request){
        //将商品信息添加到购物车
        //使用工具类获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        //保存购物车信息
        cartFeignClient.addCart(skuId, skuNum);
        //拿到商品的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    //购物车页面
    @RequestMapping("/cart.html")
    public String cart(){
        return "cart/index";
    }

}
