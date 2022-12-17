package com.atguigu;

import com.atguigu.entity.UserAddress;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "shop-user")
public interface UserFeignClient {
    @GetMapping("/user/getAddressByUserId/{userId}")
    public List<UserAddress> getAddressByUserId(@PathVariable String userId);
}
