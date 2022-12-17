package com.atguigu.controller;

import com.atguigu.OrderFeignClient;
import com.atguigu.entity.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebPayController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("/pay.html")
    public String pay(@RequestParam Long orderId, Model model){
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndDetail(orderId);
        model.addAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }
    //支付成功的页面
    @GetMapping("/alipay/success.html")
    public String alipaySuccess(){
        return "payment/success";
    }
}
