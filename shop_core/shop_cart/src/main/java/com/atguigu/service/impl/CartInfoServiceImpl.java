package com.atguigu.service.impl;

import com.atguigu.ProductFeignClient;
import com.atguigu.constant.RedisConst;
import com.atguigu.dao.CartInfoMapper;
import com.atguigu.entity.CartInfo;
import com.atguigu.entity.SkuInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.CartInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 用户登录系统时更新冗余 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-14
 */
@Service
public class CartInfoServiceImpl extends ServiceImpl<CartInfoMapper, CartInfo> implements CartInfoService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Override
    public RetVal addCart(String oneOfUserId, Long skuId, Integer skuNum) {
        //查询缓存中是否有此购物车的信息
        BoundHashOperations hashOps = getRedisOptionByUserId(oneOfUserId);
        boolean isExist = hashOps.hasKey(skuId.toString());
        if (isExist){
            //如果购物车中存在购物信息,则数量加一
            CartInfo redisCartInfo = (CartInfo) hashOps.get(skuId.toString());
            redisCartInfo.setSkuNum(redisCartInfo.getSkuNum()+skuNum);
            //如果后台修改了商品的价格,购物车中的商品价格也要发生改变
            redisCartInfo.setCartPrice(productFeignClient.getSkuPrice(skuId));
            redisCartInfo.setUpdateTime(new Date());
            hashOps.put(skuId.toString(),redisCartInfo);
        }else{
            //如果购物车中不存在购物信息,则在购物车中添加购物信息
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(oneOfUserId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setRealTimePrice(skuInfo.getPrice());
            cartInfo.setIsChecked(1);
            //此处要给购物车一个新建时间
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
            hashOps.put(skuId.toString(),cartInfo);
            //根据传入的用户Id设置购物车的过期时间
            setExpireTime(oneOfUserId);
        }

        return RetVal.ok();
    }

    //获取购物车列表
    @SneakyThrows
    @Override
    public RetVal getCartList(String userId, String userTempId) {
        String oneOfUserId= "";
        //1.userTempId中有购物车信息,userId中没有购物车信息
        if (StringUtils.isEmpty(userId) && !StringUtils.isEmpty(userTempId)){
            oneOfUserId = userTempId;
        }
        //2.userTempId中没有购物车信息,userId中有购物车信息
        if (!StringUtils.isEmpty(userId)){
            oneOfUserId = userId;
            //查询未登录时购物车项中是否有信息
            BoundHashOperations hashOperation = getRedisOptionByUserId(oneOfUserId);
            Set keys = hashOperation.keys();
            if (!CollectionUtils.isEmpty(keys)){
                //不为空说明原来购物车中有信息,需要合并
                mergeCartInfoList(userId,userTempId);
            }
        }
        List<CartInfo> retCartInfoList = queryCartInfoFromRedis(oneOfUserId);
        return RetVal.ok(retCartInfoList);
    }
    //判断是否勾选购物车项
    @Override
    public void checkCart(String oneOfUserId, Long skuId, Integer isChecked) {
        //从redis中获取数据
        BoundHashOperations hashOperations = getRedisOptionByUserId(oneOfUserId);
        if (hashOperations.hasKey(skuId.toString())){
            CartInfo cartInfo = (CartInfo) hashOperations.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            //更新redis
            hashOperations.put(skuId.toString(),cartInfo);
            //更新一下过期时间
            setExpireTime(oneOfUserId);
        }
    }
    //删除购物车中的购物项
    @Override
    public void deleteCart(Long skuId, String oneOfUserId) {
        BoundHashOperations operations = getRedisOptionByUserId(oneOfUserId);
        if (operations.hasKey(skuId.toString())){
            operations.delete(skuId.toString());
        }
    }

    @Override
    public List<CartInfo> getSelectedCartInfo(String userId) {
        BoundHashOperations option = getRedisOptionByUserId(userId);
        List<CartInfo> cartInfoList = option.values();
        if (!CollectionUtils.isEmpty(cartInfoList)){
            List<CartInfo> selectCartList = new ArrayList<>();
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getIsChecked() == 1){
                    //能进来说明是选中的商品
                    selectCartList.add(cartInfo);
                }
            }
            return selectCartList;
        }
        return null;
    }

    private List<CartInfo> queryCartInfoFromRedis(String oneOfUserId) throws ExecutionException, InterruptedException {
        BoundHashOperations hashOperations = getRedisOptionByUserId(oneOfUserId);
        List<CartInfo> cartInfoList = hashOperations.values();
        //异步修改购物车中的价格
        CompletableFuture<List<CartInfo>> realTimePriceFuture = CompletableFuture.supplyAsync(()->{
            updateCartRealTimePrice(oneOfUserId, cartInfoList);
            return cartInfoList;
        });
        CompletableFuture<List<CartInfo>> completableFuture = realTimePriceFuture.thenApplyAsync(acceptVal->{
            return acceptVal.stream().sorted(Comparator.comparing(CartInfo::getCreateTime).reversed()).collect(Collectors.toList());
        });
        List<CartInfo> retCartInfoList = completableFuture.get();
        return retCartInfoList;
    }

    private void updateCartRealTimePrice(String oneOfUserId, List<CartInfo> cartInfoList) {
        BoundHashOperations operations = getRedisOptionByUserId(oneOfUserId);
        for (CartInfo cartInfo : cartInfoList) {
            Long skuId = cartInfo.getSkuId();
            //获取实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            if (!cartInfo.getRealTimePrice().equals(skuPrice)){
                cartInfo.setCartPrice(skuPrice);
                operations.put(skuId.toString(),cartInfo);
            }
        }

    }

    private void mergeCartInfoList(String userId, String userTempId) {
        //未登录时的购物车项信息
        BoundHashOperations tempOperation = getRedisOptionByUserId(userTempId);
        List<CartInfo> noLoginCartInfoList = tempOperation.values();
        //登录时的购物车项
        BoundHashOperations userIdOperation = getRedisOptionByUserId(userId);
        List<CartInfo> loginCartInfoList = userIdOperation.values();
        //创建一个map,把登录过的购物车项放入到map中
        Map<String, CartInfo> loginMap = new HashMap<>();
        for (CartInfo loginCartInfo : loginCartInfoList) {
            loginMap.put(loginCartInfo.getSkuId().toString(),loginCartInfo);
        }
        //遍历未登录时的购物车项判断是否存在同类信息,存在一样的就增加,不存在就添加
        for (CartInfo noLoginCartInfo : noLoginCartInfoList) {
            String noLoginSkuId = noLoginCartInfo.getSkuId().toString();
            if (loginMap.containsKey(noLoginSkuId)){
                //在这说明存在
                CartInfo cartInfo = loginMap.get(noLoginSkuId);
                cartInfo.setSkuNum(cartInfo.getSkuNum() + noLoginCartInfo.getSkuNum());
                if (noLoginCartInfo.getIsChecked()==0){
                    cartInfo.setIsChecked(1);
                }
            }else{
             //在这说明不存在
             noLoginCartInfo.setUserId(userId);
             //把未登录的购物车添加到已经登录的购物车项里边
             loginMap.put(noLoginSkuId,noLoginCartInfo);
            }
        }
        userIdOperation.putAll(loginMap);
        //临时的购物车要删除
        String userCartKey = getUserCartKey(userTempId);
        redisTemplate.delete(userCartKey);
    }

    //根据传入的用户Id设置购物车的过期时间
    private void setExpireTime(String oneOfUserId) {
        String userCartKey = getUserCartKey(oneOfUserId);
        redisTemplate.expire(userCartKey,RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

    }

    //根据用户id得到redis的操作对象
    private BoundHashOperations getRedisOptionByUserId(String oneOfUserId) {
        String userCartKey = getUserCartKey(oneOfUserId);
        return redisTemplate.boundHashOps(userCartKey);
    }
    //得到用户购物车
    private String getUserCartKey(String oneOfUserId) {
        //user:3:cart
        String userCartKey= RedisConst.USER_KEY_PREFIX+ oneOfUserId +RedisConst.USER_CART_KEY_SUFFIX;
        return userCartKey;
    }
}
