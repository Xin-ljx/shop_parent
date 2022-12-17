package com.atguigu.controller;

import com.atguigu.OrderFeignClient;
import com.atguigu.constant.MqConst;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.OrderInfo;
import com.atguigu.entity.PrepareSeckillOrder;
import com.atguigu.entity.SeckillProduct;
import com.atguigu.entity.UserSeckillSkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.result.RetValCodeEnum;
import com.atguigu.service.SeckillProductService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MD5;
import com.atguigu.utils.DateUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/seckill")
public class SecKillController {
    @Autowired
    private SeckillProductService seckillProductService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignClient orderFeignClient;
    //1.秒杀商品列表
    @GetMapping("/queryAllSecKillProduct")
    public List<SeckillProduct> queryAllSecKillProduct(){
        return seckillProductService.queryAllSecKillProduct();
    }
    //2.秒杀商品详情信息
    @GetMapping("/querySecKillBySkuId/{skuId}")
    public SeckillProduct querySecKillBySkuId(@PathVariable Long skuId){
        return seckillProductService.querySecKillBySkuId(skuId);
    }
    //3.生成抢购码
    @GetMapping("/generateSeckillCode/{skuId}")
    public RetVal generateSeckillCode(@PathVariable Long skuId, HttpServletRequest request){
        //判断用户是否登陆
        String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId)){
            //从缓存中取出秒杀商品的信息
            SeckillProduct seckillProduct = seckillProductService.querySecKillBySkuId(skuId);
            //判断当前时间是否在秒杀范围之内
            //生成一个抢购码
            Date nowTime = new Date();
            //判断当前时间是否在秒杀范围之内
            if (DateUtil.dateCompare(seckillProduct.getStartTime(),nowTime) && DateUtil.dateCompare(nowTime,seckillProduct.getEndTime())){
                //生成一个抢购码
                String secKillCode = MD5.encrypt(userId);
                return RetVal.ok(secKillCode);
            }
        }
        return RetVal.fail().message("获取抢购码失败,请您先登陆");
    }
    //4.秒杀预下单http://api.gmall.com/seckill/prepareSeckill/33?seckillCode=eccbc87e4b5ce2fe28308fd9f2a7baf3
    @PostMapping("/prepareSeckill/{skuId}")
    public RetVal prepareSeckill(@PathVariable Long skuId, String seckillCode, HttpServletRequest request){
        //判断抢购码是否正确
        String userId = AuthContextHolder.getUserId(request);
        if (!StringUtils.isEmpty(userId)){
            if (!MD5.encrypt(userId).equals(seckillCode)){
                //预购码不正确
                return RetVal.build(null, RetValCodeEnum.SECKILL_ILLEGAL);
            }
            //校验秒杀状态位
            String state = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_STATE_PREFIX + skuId);
            if (StringUtils.isEmpty(state)){
                //能进来说明状态位为空
                return RetVal.build(null,RetValCodeEnum.SECKILL_ILLEGAL);
            }
            //秒杀状态位为1才可以进行秒杀 就发一个消息给MQ 生成一个秒杀预下单 起到一个销峰的作用
            if (RedisConst.CAN_SECKILL.equals(state)){
                UserSeckillSkuInfo userSeckillSkuInfo = new UserSeckillSkuInfo();
                userSeckillSkuInfo.setUserId(userId);
                userSeckillSkuInfo.setSkuId(skuId);
                rabbitTemplate.convertAndSend(MqConst.PREPARE_SECKILL_EXCHANGE,MqConst.PREPARE_SECKILL_ROUTE_KEY,userSeckillSkuInfo);
            }else{
                //通知其他节点改变秒杀位状态
                redisTemplate.convertAndSend(RedisConst.PREPARE_PUB_SUB_SECKILL,skuId+":"+RedisConst.CAN_NOT_SECKILL);
                return RetVal.build(null,RetValCodeEnum.SECKILL_FINISH);
            }
        }
        return RetVal.ok();
    }
    //判断是否具有抢购资格
    //http://api.gmall.com/seckill/hasQualified/24
    @GetMapping("/hasQualified/{skuId}")
    public RetVal hasQualified(@PathVariable Long skuId,HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.hasQualified(skuId,userId);
    }
    //6.返回秒杀确认页面需要的数据信息
    @GetMapping("/secKillConfirm")
    public RetVal secKillConfirm(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillProductService.secKillConfirm(userId);
    }
    //提交订单
    //http://api.gmall.com/seckill/submitSecKillOrder
    @PostMapping("/submitSecKillOrder")
    public RetVal submitSecKillOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        //a.获取用户的临时订单信息
        PrepareSeckillOrder prepareSeckillOrder = (PrepareSeckillOrder) redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).get(userId);
        if (prepareSeckillOrder==null){
            return RetVal.fail().message("非法请求");
        }
        //b.通过远程feign保存订单与订单基本信息
        Long orderId = orderFeignClient.saveOrderAndDetail(orderInfo);
        if (orderId==null){
            return RetVal.fail().message("下单失败");
        }
        //c删除redis中的预购单信息
        redisTemplate.boundHashOps(RedisConst.PREPARE_SECKILL_USERID_ORDER).delete(userId);
        //d.把该用户购买的信息放到redis里面 用于判断该用户是否购买过该商品
        redisTemplate.boundHashOps(RedisConst.BOUGHT_SECKILL_USER_ORDER).put(userId,orderId);
        return RetVal.ok();
    }
}
