package com.atguigu.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.constant.RedisConst;
import com.atguigu.entity.UserAddress;
import com.atguigu.entity.UserInfo;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserAddressService;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.IpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-13
 */
@RestController
@RequestMapping("/user")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserAddressService userAddressService;

    @PostMapping("/login")
    public RetVal login(@RequestBody UserInfo uiUserInfo, HttpServletRequest request){
        //根据用户信息查询数据库看用户信息是否存在
        UserInfo userInfoDb = userInfoService.queryUserFromDb(uiUserInfo);
        // 把用户信息放入到缓存当中
        if (userInfoDb!=null){
            String token = UUID.randomUUID().toString();
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
            JSONObject userInfoJson = new JSONObject();
            userInfoJson.put("userId",userInfoDb.getId());
            userInfoJson.put("loginIp", IpUtil.getIpAddress(request));
            redisTemplate.opsForValue().set(userKey,userInfoJson,RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //生成一个token信息,并返回相关信息给前端
            HashMap<String, Object> retMap = new HashMap<>();
            retMap.put("token",token);
            String nickName = userInfoDb.getNickName();
            retMap.put("nickName",nickName);
            return RetVal.ok(retMap);
        }else{
            return RetVal.fail().message("登录失败");
        }
    }

    //退出登录
    @GetMapping("logout")
    public RetVal logout(HttpServletRequest request) {
        String token =request.getHeader("token");
//        在这个地方通过request没有拿到cookies
//        Cookie[] cookies = request.getCookies();
//        for (Cookie cookie : cookies) {
//            if ("token".equals(cookie.getName())) {
//                token = cookie.getValue();
//            }
//        }
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        redisTemplate.delete(userKey);
        return RetVal.ok();
    }

    //根据userId获取用户地址信息
    @GetMapping("getAddressByUserId/{userId}")
    public List<UserAddress> getAddressByUserId(@PathVariable String userId){
        QueryWrapper<UserAddress> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",userId);
        return userAddressService.list(wrapper);
    }
}

