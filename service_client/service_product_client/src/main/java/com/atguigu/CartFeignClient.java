package com.atguigu;

import com.atguigu.entity.CartInfo;
import com.atguigu.result.RetVal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(value = "shop-cart")
public interface CartFeignClient {
    @PostMapping("/cart/addToCart/{skuId}/{skuNum}")
    public RetVal addCart(@PathVariable Long skuId,
                          @PathVariable Integer skuNum);

    @GetMapping("/cart/getSelectedCartInfo/{userId}")
    public List<CartInfo> getSelectedCartInfo(@PathVariable String userId);
}
