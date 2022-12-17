package com.atguigu.controller;


import com.atguigu.CartFeignClient;
import com.atguigu.UserFeignClient;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.UserAddress;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 订单表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-15
 */
@RestController
@RequestMapping("/order")
public class OrderInfoController {
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    //订单的确认信息
    @GetMapping("/confirm")
    public RetVal confirm(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //1.获取收货人的地址信息
        List<UserAddress> userAddressList = userFeignClient.getAddressByUserId(userId);
        //2.购物车清单信息
        List<CartInfo> selectedCartInfoList = cartFeignClient.getSelectedCartInfo(userId);
        int totalNum = 0;
        BigDecimal totalMoney = new BigDecimal("0");
        //2.1把购物车信息转换为订单详情信息,创建一个集合接收参数
        List<OrderDetail> detailArrayList = new ArrayList<>();
        if (!StringUtils.isEmpty(selectedCartInfoList)){
            for (CartInfo cartInfo : selectedCartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetail.setSkuNum(cartInfo.getSkuNum()+"");
                //获取订单的总金额
                totalMoney = totalMoney.add(cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())));
                totalNum += cartInfo.getSkuNum();
                detailArrayList.add(orderDetail);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",detailArrayList);
        map.put("totalNum",totalNum);
        map.put("totalMoney",totalMoney);
        //生成一个流水号解决订单重复提交的问题
        String tradeNo = orderInfoService.generateTradeNo(userId);
        map.put("tradeNo",tradeNo);
        return RetVal.ok(map);
    }

    //提交订单信息
    //http://api.gmall.com/order/submitOrder?tradeNo=null
    @PostMapping("/submitOrder")
    public RetVal submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //保存订单之前做判断,判断两个订单号是否相同
        String uiTradeNo = request.getParameter("tradeNo");
        boolean flag = orderInfoService.checkTradeNo(uiTradeNo,userId);
        if (!flag){
            return RetVal.fail().message("不可以重复提交订单哦");
        }
        //在保存订单之前,验证商品的价格和库存
        String warningMsg = orderInfoService.checkPriceAndStock(orderInfo);
        if (!StringUtils.isEmpty(warningMsg)){
            return RetVal.fail().message(warningMsg);
        }
        //保存订单的基本信息和详细信息
        orderInfo.setUserId(Long.parseLong(userId));
        Long orderId = orderInfoService.saveOrderAndDetail(orderInfo);
        //提交订单后要在redis中删除流水号
        orderInfoService.deleteTreadNo(userId);
        return RetVal.ok(orderId);
    }
    //获取订单信息以及订单的详细信息
    @GetMapping("/getOrderInfoAndDetail/{orderId}")
    public OrderInfo getOrderInfoAndDetail(@PathVariable Long orderId){
        return orderInfoService.getOrderInfoAndDetail(orderId);
    }

    //拆单
    @PostMapping("/splitOrder")
    public String splitOrder(@RequestParam Long orderId,@RequestParam String wareHouseIdSkuIdMapJson){
        return orderInfoService.splitOrder(orderId,wareHouseIdSkuIdMapJson);
    }

    //5.保存订单及订单基本信息
    @PostMapping("/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo){
        return orderInfoService.saveOrderAndDetail(orderInfo);
    }
}

