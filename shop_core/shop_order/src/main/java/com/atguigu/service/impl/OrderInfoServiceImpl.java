package com.atguigu.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.ProductFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.dao.OrderInfoMapper;
import com.atguigu.entity.OrderDetail;
import com.atguigu.entity.OrderInfo;
import com.atguigu.enums.OrderStatus;
import com.atguigu.enums.ProcessStatus;
import com.atguigu.service.OrderDetailService;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.HttpClientUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 订单表 订单表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-15
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${cancel.order.delay}")
    private Integer cancelOrderDelay;
    @Override
    @Transactional
    public Long saveOrderAndDetail(OrderInfo orderInfo) {
        //设置订单的基本信息和详细信息
        String outTradeNo = "atguigu" + UUID.randomUUID();
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        orderInfo.setTradeBody("我去年买了个表");
        //订单过期时间
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE,15);
        orderInfo.setExpireTime(instance.getTime());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        baseMapper.insert(orderInfo);
        //保存订单详情
        Long orderId = orderInfo.getId();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderId);
            }
            orderDetailService.saveBatch(orderDetailList);
        }
        //在保存订单之后发送一个延迟消息,超出时间自动取消订单
        rabbitTemplate.convertAndSend(MqConst.CANCEL_ORDER_EXCHANGE,MqConst.CANCEL_ORDER_ROUTE_KEY,orderId,
                correlationData->{
                    correlationData.getMessageProperties().setDelay(cancelOrderDelay);
                    return correlationData;
                });
        return orderId;
    }

    @Override
    public String generateTradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString();
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }
    //删除流水号
    @Override
    public void deleteTreadNo(String userId) {
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkTradeNo(String uiTradeNo, String userId) {
        String redisTradeNo = (String) redisTemplate.opsForValue().get("user:" + userId + ":tradeNo");
        return uiTradeNo.equals(redisTradeNo);
    }

    @Override
    public String checkPriceAndStock(OrderInfo orderInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        //获取商品清单
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                Long skuId = orderDetail.getSkuId();
                String skuName = orderDetail.getSkuName();
                String skuNum = orderDetail.getSkuNum();
                BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
                if (orderPrice.compareTo(skuPrice)!=0){
                    stringBuilder.append(skuName).append("商品价格有变化,请刷新页面");
                }
                //判断库存是否够
                //http://localhost:8100/hasStock?skuId=24&num=97
                String url = "http://localhost:8100/hasStock?skuId="+skuId+"&num="+ skuNum;
                String result = HttpClientUtil.doGet(url);
                //0为没有库存,1为有库存
                if ("0".equals(result)){
                    stringBuilder.append(skuName).append("库存不足,店家正在火速补货");
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public OrderInfo getOrderInfoAndDetail(Long orderId) {
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        if (orderInfo!= null){
            QueryWrapper<OrderDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("order_id",orderId);
            List<OrderDetail> list = orderDetailService.list(wrapper);
            orderInfo.setOrderDetailList(list);
        }
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(OrderInfo orderInfo, ProcessStatus status) {
        orderInfo.setProcessStatus(status.name());
        //修改订单的状态---主要是给客户观看
        orderInfo.setOrderStatus(status.getOrderStatus().name());
        baseMapper.updateById(orderInfo);
    }
    //给库存系统发送减库存消息
    @Override
    public void sendMsgToWareManage(OrderInfo orderInfo) {
        //更新进度,方便管理员管理
        updateOrderStatus(orderInfo,ProcessStatus.NOTIFIED_WARE);
        HashMap<String, Object> dataMap = assembleWareHouseData(orderInfo);
        String jsonData = JSONObject.toJSONString(dataMap);
        rabbitTemplate.convertAndSend(MqConst.DECREASE_STOCK_EXCHANGE,MqConst.DECREASE_STOCK_ROUTE_KEY,jsonData);
    }

    //拆单
    @Override
    public String splitOrder(Long orderId, String wareHouseIdSkuIdMapJson) {
        //获取到原始的订单
        OrderInfo parentOrderInfo = getOrderInfoAndDetail(orderId);
        List<Map<String,Object>> assembleWareHouseDataList = new ArrayList<>();
        //[{"wareHouseId":"1","skuIdList":["24"]},{"wareHouseId":"2","skuIdList":["30"]}]
        List<Map> wareHouseIdSkuIdMapList = JSON.parseArray(wareHouseIdSkuIdMapJson, Map.class);
        for (Map wareHouseIdSkuIdMap : wareHouseIdSkuIdMapList) {
            //获取仓库的id
            String wareHoseId = (String) wareHouseIdSkuIdMap.get("wareHoseId");
            //获取子订单所拥有的的订单详情
            List<String> skuIdList = (List<String>) wareHouseIdSkuIdMap.get("skuIdList");
            //设置子订单与子订单间的基本信息
            OrderInfo childOrderInfo = new OrderInfo();
            BeanUtils.copyProperties(parentOrderInfo,childOrderInfo);
            childOrderInfo.setId(null);
            childOrderInfo.setParentOrderId(orderId);
            childOrderInfo.setWareHouseId(wareHoseId);
            List<OrderDetail> parentDetailList = parentOrderInfo.getOrderDetailList();
            //新建一个子订单list接收子订单信息
            List<OrderDetail> childDetailList = new ArrayList<>();
            BigDecimal childTotalMoney = new BigDecimal(0);
            if(!CollectionUtils.isEmpty(parentDetailList)){
                for (OrderDetail parentDetail : parentDetailList) {
                    for (String skuId : skuIdList) {
                        //判断该仓库的skuId是否跟本仓库的skuId相同,如果相同说明是子订单的订单详情
                        if (parentDetail.getSkuId()==Long.parseLong(skuId)){
                            BigDecimal orderPrice = parentDetail.getOrderPrice();
                            String skuNum = parentDetail.getSkuNum();
                            childTotalMoney = childTotalMoney.add(orderPrice.multiply(new BigDecimal(skuNum)));
                            childDetailList.add(parentDetail);
                        }
                    }
                }
            }
            //将子订单详情放入到订单信息中
            childOrderInfo.setOrderDetailList(childDetailList);
            //设置子订单的价格
            childOrderInfo.setTotalMoney(childTotalMoney);
            //保存子订单与子订单的详细信息
            saveOrderAndDetail(childOrderInfo);
            HashMap<String, Object> dataMap = assembleWareHouseData(childOrderInfo);
            assembleWareHouseDataList.add(dataMap);
        }
        //拆单完成之后把原始订单改为split
        updateOrderStatus(parentOrderInfo,ProcessStatus.SPLIT);
        //拆单完成之后还需要返回信息给仓库系统
 /*       rabbitTemplate.convertAndSend(MqConst.EXCHANGE);*/

        return JSONObject.toJSONString(assembleWareHouseDataList);
    }

    private HashMap<String, Object> assembleWareHouseData(OrderInfo orderInfo) {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("orderId",orderInfo.getId());
        dataMap.put("consignee",orderInfo.getConsignee());
        dataMap.put("consigneeTel",orderInfo.getConsigneeTel());
        dataMap.put("orderComment",orderInfo.getOrderComment());
        dataMap.put("orderBody",orderInfo.getTradeBody());
        dataMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
        dataMap.put("paymentWay",2);
        //设置仓库id仓库用来拆单
        if (!StringUtils.isEmpty(orderInfo.getWareHouseId())){
            dataMap.put("wareId",orderInfo.getWareHouseId());
        }
        //商品清单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<Map<String,Object>> detailMapList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailMapList.add(detailMap);
        }
        dataMap.put("details",detailMapList);

        return dataMap;
    }
}
