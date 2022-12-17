package com.atguigu.controller;


import com.atguigu.constant.MqConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-23
 */
@RestController
public class SimulateQuartzController {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @GetMapping("sendMsgToScanSeckill")
    public String sendMsgToScanSeckill(){
        rabbitTemplate.convertAndSend(MqConst.SCAN_SECKILL_EXCHANGE,MqConst.SCAN_SECKILL_ROUTE_KEY,"");
        return "success";
    }
}

