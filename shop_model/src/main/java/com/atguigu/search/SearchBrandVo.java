package com.atguigu.search;

import lombok.Data;

import java.io.Serializable;

// 品牌数据
@Data
public class SearchBrandVo implements Serializable {
    //品牌id
    private Long brandId;
    //品牌名称
    private String brandName;
    //品牌logo地址
    private String brandLogoUrl;
}

