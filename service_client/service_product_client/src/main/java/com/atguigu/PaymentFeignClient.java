package com.atguigu;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(value = "shop-payment")
public interface PaymentFeignClient {
    @RequestMapping("/payment/createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId);

    //支付成功之后的异步请求
    @PostMapping("/payment/async/notify")
    public String asyncNotify(@RequestBody Map<String,String> alipayParam);

    //退款接口
    @GetMapping("/payment/refund/{orderId}")
    public Boolean refund(@PathVariable Long orderId);
    //查询支付宝中是否有交易记录
    @GetMapping("/payment/queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId);
    //关闭交易
    @GetMapping("/payment/close/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId);
}
