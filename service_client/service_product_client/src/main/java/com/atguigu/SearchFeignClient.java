package com.atguigu;

import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "shop-search")
public interface SearchFeignClient {
    //商品上架
    @GetMapping("/search/onSale/{skuId}")
    public String onSale(@PathVariable Long skuId);
    //商品下架
    @GetMapping("/search/offSale/{skuId}")
    public String offSale(@PathVariable Long skuId);

    //商品搜索
    @PostMapping("/search/searchProduct")
    public RetVal searchProduct(SearchParam searchParam);

    //热点数据
    @GetMapping("/search/incrHotScore/{skuId}")
    public String incrHotScore(@PathVariable Long skuId);
}
