package com.atguigu.controller;

import com.atguigu.dao.ProductRepository;
import com.atguigu.result.RetVal;
import com.atguigu.search.Product;
import com.atguigu.search.SearchParam;
import com.atguigu.search.SearchResponseVo;
import com.atguigu.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private ElasticsearchRestTemplate esTemplate;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ProductRepository productRepository;
    //创建索引
    @GetMapping("/createIndex")
    public String createIndex(){
        esTemplate.createIndex(Product.class);
        esTemplate.putMapping(Product.class);
        return "success";
    }
    //商品上架
    @GetMapping("/onSale/{skuId}")
    public String onSale(@PathVariable Long skuId){
        searchService.onSale(skuId);
        return "success";
    }
    //商品下架
    @GetMapping("/offSale/{skuId}")
    public String offSale(@PathVariable Long skuId){
        searchService.offSale(skuId);
        return "success";
    }
    //搜索
    @PostMapping("searchProduct")
    public RetVal searchProduct(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo = searchService.searchProduct(searchParam);
        return RetVal.ok(searchResponseVo);
    }
    //热点数据
    @GetMapping("incrHotScore/{skuId}")
    public String incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return "success";
    }
}
