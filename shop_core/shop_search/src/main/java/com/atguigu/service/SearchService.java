package com.atguigu.service;

import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;

public interface SearchService {
    void onSale(Long skuId);

    void offSale(Long skuId);

    SearchResponseVo searchProduct(SearchParam searchParam);

    void incrHotScore(Long skuId);
}
