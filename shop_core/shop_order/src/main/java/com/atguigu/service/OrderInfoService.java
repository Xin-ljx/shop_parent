package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.ProcessStatus;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单表 订单表 服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-15
 */
public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderAndDetail(OrderInfo orderInfo);

    String generateTradeNo(String userId);

    void deleteTreadNo(String userId);

    boolean checkTradeNo(String uiTradeNo, String userId);

    String checkPriceAndStock(OrderInfo orderInfo);

    OrderInfo getOrderInfoAndDetail(Long orderId);

    void updateOrderStatus(OrderInfo orderInfo, ProcessStatus paid);

    void sendMsgToWareManage(OrderInfo orderInfo);

    String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson);
}
