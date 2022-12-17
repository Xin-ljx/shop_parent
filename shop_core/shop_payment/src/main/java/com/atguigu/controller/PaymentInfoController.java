package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.config.AlipayConfig;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.service.PaymentInfoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 支付信息表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-21
 */
@RestController
@RequestMapping("/payment")
public class PaymentInfoController {
    @Autowired
    private PaymentInfoService paymentInfoService;

    //http://api.gmall.com/payment/createQrCode/129
    @RequestMapping("/createQrCode/{orderId}")
    public String createQrCode(@PathVariable Long orderId){
        return paymentInfoService.createQrCode(orderId);
    }

    //支付成功之后的异步请求
    @PostMapping("/async/notify")
    @SneakyThrows
/*    //支付宝传回来的信息是map结构,支付宝调用我们的方式
    public String asyncNotify(@RequestParam Map<String,String> alipayParam){
        System.out.println(alipayParam);
        System.out.println(JSONObject.toJSONString(alipayParam));
        return "success";
    }    */
    //我们调用自己的方式,省钱
    public String asyncNotify(@RequestBody Map<String,String> alipayParam){
        //System.out.println(JSONObject.toJSONString(alipayParam));
        //将异步请求中收到的所有参数放到map中,调用SDK签名
        boolean signVerified = AlipaySignature.rsaCheckV1(alipayParam, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        if (signVerified){
            //支付宝验签成功
            String tradeStatus = alipayParam.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                String outTradeNo = alipayParam.get("out_trade_no");
                PaymentInfo paymentInfo= paymentInfoService.getPaymentInfo(outTradeNo);
                String paymentStatus = paymentInfo.getPaymentStatus();
                if (paymentStatus.equals(PaymentStatus.PAID.name()) || paymentStatus.equals(PaymentStatus.ClOSED.name())){
                    return "success";
                }
            }
            //修改支付表中的信息
            System.out.println(alipayParam);
            System.out.println(JSONObject.toJSONString(alipayParam));
            paymentInfoService.updatePaymentInfo(alipayParam);
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
        }
        return "success";
    }

    //退款接口
    @GetMapping("refund/{orderId}")
    public Boolean refund(@PathVariable Long orderId){
        return paymentInfoService.refund(orderId);
    }
    //查询支付宝中是否有交易记录
    @GetMapping("queryAlipayTrade/{orderId}")
    public Boolean queryAlipayTrade(@PathVariable Long orderId){
        return paymentInfoService.queryAlipayTrade(orderId);
    }
    //关闭交易
    @GetMapping("close/{orderId}")
    public Boolean closeAlipayTrade(@PathVariable Long orderId){
        return paymentInfoService.closeAlipayTrade(orderId);
    }
}

