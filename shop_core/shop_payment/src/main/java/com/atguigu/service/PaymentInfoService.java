package com.atguigu.service;

import com.atguigu.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 支付信息表 服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-21
 */
public interface PaymentInfoService extends IService<PaymentInfo> {

    String createQrCode(Long orderId);

    PaymentInfo getPaymentInfo(String outTradeNo);

    void updatePaymentInfo(Map<String, String> alipayParam);


    Boolean refund(Long orderId);

    boolean queryAlipayTrade(Long orderId);

    boolean closeAlipayTrade(Long orderId);
}
