package com.atguigu.controller;


import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-14
 */
@RestController
@RequestMapping("/cart")
public class CartInfoController {
    @Autowired
    private CartInfoService cartInfoService;
    //1.加入购物车
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId,
                          @PathVariable Integer skuNum,
                          HttpServletRequest request){
        String oneOfUserId = "";
        //还差一个用户id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            oneOfUserId = AuthContextHolder.getUserTempId(request);
        }else{
            oneOfUserId = userId;
        }
        cartInfoService.addCart(oneOfUserId,skuId,skuNum);
        return RetVal.ok();
    }
    //2.购物车列表
    //http://api.gmall.com/cart//getCartList
    @GetMapping("/getCartList")
    public RetVal getCartList(HttpServletRequest request){
        //获取购物车列表要获取到登录用户的id和为登录用户id的购物车信息并且将其合并
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        return cartInfoService.getCartList(userId,userTempId);
    }
    //3.是否勾选购物车商品
    //http://api.gmall.com/cart//checkCart/26/1
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public RetVal checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        String oneOfUserId = "";
        //获取userId,从request域中
        String userId = AuthContextHolder.getUserId(request);
        //判断userId是否为空
        if (!StringUtils.isEmpty(userId)){
            oneOfUserId = userId;
        }else{
            oneOfUserId = AuthContextHolder.getUserTempId(request);
        }
        cartInfoService.checkCart(oneOfUserId,skuId,isChecked);
        return RetVal.ok();
    }
    //删除购物车项
    //http://api.gmall.com/cart//deleteCart/26
    @DeleteMapping("deleteCart/{skuId}")
    public RetVal deleteCart(@PathVariable Long skuId,HttpServletRequest request){
        //差用户id,要知道用户id才可以删除购物车
        String oneOfUserId = "";
        String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId)){
            oneOfUserId = userId;
        }else{
            oneOfUserId = AuthContextHolder.getUserTempId(request);
        }
        cartInfoService.deleteCart(skuId,oneOfUserId);
        return RetVal.ok();
    }
    //获取已选中的购物车项
    @GetMapping("/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId){
        return cartInfoService.getSelectedCartInfo(userId);
    }

}

