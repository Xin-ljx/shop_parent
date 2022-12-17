package com.atguigu.controller;


import com.atguigu.dao.TOrder1Mapper;
import com.atguigu.entity.TOrder;
import com.atguigu.entity.TOrderDetail;
import com.atguigu.enums.OrderStatus;
import com.atguigu.service.TOrder1Service;
import com.atguigu.service.TOrderDetail1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-20
 */
@RestController
@RequestMapping("/order")
public class TOrderController {

    @Autowired
    private TOrder1Service tOrder1Service;
    @Autowired
    private TOrderDetail1Service tOrderDetail1Service;
    @Autowired
    private TOrder1Mapper order1Mapper;
    //保存订单分库分表
    @GetMapping("test01/{loopNum}")
    public void test01(@PathVariable Integer loopNum){
        for (int i = 0; i < loopNum; i++) {
            TOrder tOrder = new TOrder();
            tOrder.setOrderStatus(OrderStatus.UNPAID.name());
            tOrder.setOrderPrice(99);
            String tradeNo = UUID.randomUUID().toString();
            tOrder.setTradeNo(tradeNo);

            //设置分库分表的依据
            int userId = new Random().nextInt(20);
            tOrder.setUserId(Long.parseLong(userId+""));
            System.out.println("用户id"+userId);
            tOrder1Service.save(tOrder);
        }

    }
    //2.保存订单基本信息,和详情信息分库分表
    @GetMapping("test02/{userId}")
    public void test02(@PathVariable Long userId){
        TOrder tOrder = new TOrder();
        tOrder.setTradeNo("enjoy6288");
        tOrder.setOrderPrice(9900);
        tOrder.setUserId(userId);//ds-1,table_4
        tOrder.setOrderStatus("未支付");
        tOrder1Service.save(tOrder);

        TOrderDetail iphone13 = new TOrderDetail();
        iphone13.setOrderId(tOrder.getId());
        iphone13.setSkuName("Iphone13");
        iphone13.setSkuNum(1);
        iphone13.setSkuPrice(6000);
        iphone13.setUserId(userId);
        tOrderDetail1Service.save(iphone13);

        TOrderDetail sanxin = new TOrderDetail();
        sanxin.setOrderId(tOrder.getId());
        sanxin.setSkuName("三星");
        sanxin.setSkuNum(2);
        sanxin.setSkuPrice(3900);
        sanxin.setUserId(userId); //要进行分片计算
        tOrderDetail1Service.save(sanxin);
        System.out.println("执行完成");
    }
    //3.查询订单与订单详情
    @GetMapping("test03/{userId}")
    public void test03(@PathVariable Long userId){
        //根据用户id查询订单信息
        List<TOrder> orderList=order1Mapper.queryOrderAndDetail(userId);
        //根据订单id查询订单信息(跨库查询) 没有传递分片的键 相当慢
//        List<TOrder> orderList=order1Mapper.queryOrderAndDetail(null,1594282330908905473L);
        System.out.println(orderList);
    }
}

