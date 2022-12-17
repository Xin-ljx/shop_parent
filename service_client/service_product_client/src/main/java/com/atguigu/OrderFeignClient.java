package com.atguigu;

import com.atguigu.entity.OrderInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "shop-order")
public interface OrderFeignClient {
    //订单的确认信息
    @GetMapping("/order/confirm")
    public RetVal confirm();

    //获取订单信息以及订单的详细信息
    @GetMapping("/order/getOrderInfoAndDetail/{orderId}")
    public OrderInfo getOrderInfoAndDetail(@PathVariable Long orderId);

    @PostMapping("/order/saveOrderAndDetail")
    public Long saveOrderAndDetail(@RequestBody OrderInfo orderInfo);
}
