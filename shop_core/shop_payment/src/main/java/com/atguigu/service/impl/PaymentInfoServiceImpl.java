package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.OrderFeignClient;
import com.atguigu.config.AlipayConfig;
import com.atguigu.constant.MqConst;
import com.atguigu.dao.PaymentInfoMapper;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PaymentInfo;
import com.atguigu.enums.PaymentStatus;
import com.atguigu.enums.PaymentType;
import com.atguigu.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 * 支付信息表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-21
 */
@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Override
    @SneakyThrows
    public String createQrCode(Long orderId) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndDetail(orderId);
        //
        savePaymentInfo(orderInfo);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //异步通知
        request.setNotifyUrl(AlipayConfig.notify_payment_url);
        //同步通知
        request.setReturnUrl(AlipayConfig.return_payment_url);
        JSONObject bizContent = new JSONObject();
        String outTradeNo = orderInfo.getOutTradeNo();
        if (!StringUtils.isEmpty(outTradeNo)){
            bizContent.put("out_trade_no", outTradeNo);
        }
        BigDecimal totalMoney = orderInfo.getTotalMoney();
        bizContent.put("total_amount", totalMoney);
        bizContent.put("subject", orderInfo.getTradeBody());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            String alipayHtml = response.getBody();
            return alipayHtml;
        } else {
            System.out.println("调用失败");
            return null;
        }
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("out_trade_no",outTradeNo);
        wrapper.eq("payment_type",PaymentType.ALIPAY.name());
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void updatePaymentInfo(Map<String, String> alipayParam) {
        String outTradeNo = alipayParam.get("out_trade_no");
        PaymentInfo paymentInfo = getPaymentInfo(outTradeNo);
        //设置paymentInfo的信息
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        String tradeNo = alipayParam.get("trade_no");
        paymentInfo.setTradeNo(tradeNo);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSONObject.toJSONString(alipayParam));
        baseMapper.updateById(paymentInfo);

        //发送一个消息到shop_order确保消息一致性
        rabbitTemplate.convertAndSend(MqConst.PAY_ORDER_EXCHANGE,MqConst.PAY_ORDER_ROUTE_KEY,paymentInfo.getOrderId());
    }
    //支付宝退款接口
    @Override
    @SneakyThrows
    public Boolean refund(Long orderId) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndDetail(orderId);
        JSONObject bizContent = new JSONObject();
        bizContent.put("refund_amount", orderInfo.getTotalMoney());
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            //如果退单成功,则需要修改订单状态
            PaymentInfo paymentInfo = getPaymentInfo(orderInfo.getOutTradeNo());
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            baseMapper.updateById(paymentInfo);
            return true;
        } else {
            return false;
        }
    }
    //查询支付宝中是否有交易记录
    @Override
    @SneakyThrows
    public boolean queryAlipayTrade(Long orderId) {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        JSONObject bizContent = new JSONObject();
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndDetail(orderId);
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }
    }
    //关闭交易
    @Override
    @SneakyThrows
    public boolean closeAlipayTrade(Long orderId) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        OrderInfo orderInfo = orderFeignClient.getOrderInfoAndDetail(orderId);
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            return true;
        } else {
            return false;
        }
    }

    //保存支付单信息.;
    private void savePaymentInfo(OrderInfo orderInfo) {
        //在保存之前要进行判断库中是否有
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id",orderInfo.getId());
        queryWrapper.eq("payment_type",PaymentType.ALIPAY.name());
        Integer count = baseMapper.selectCount(queryWrapper);
        //如果返回的count大于0,说明库中有记录,直接返回
        if (count>0){
            return;
        }
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId() + "");
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());
        paymentInfo.setPaymentMoney(orderInfo.getTotalMoney());
        paymentInfo.setPaymentContent(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        baseMapper.insert(paymentInfo);
    }
}
