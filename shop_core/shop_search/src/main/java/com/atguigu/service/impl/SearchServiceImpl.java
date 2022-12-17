package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.ProductFeignClient;
import com.atguigu.dao.ProductRepository;
import com.atguigu.entity.BaseBrand;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.SkuInfo;
import com.atguigu.search.*;
import com.atguigu.service.SearchService;
import lombok.SneakyThrows;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Resource
    private ProductRepository productRepository;
    @Autowired
    private RestHighLevelClient highLevelClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void onSale(Long skuId) {
        Product product = new Product();
        //商品的基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo!=null){
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            //品牌信息
            Long brandId = skuInfo.getBrandId();
            BaseBrand baseBrand = productFeignClient.getBrandById(brandId);
            if (baseBrand!=null) {
                product.setBrandId(brandId);
                product.setBrandLogoUrl(baseBrand.getBrandLogoUrl());
                product.setBrandName(baseBrand.getBrandName());
            }
            //商品的分类信息
            Long category3Id = skuInfo.getCategory3Id();
            BaseCategoryView categoryView = productFeignClient.getCategoryView(category3Id);
            if (categoryView!=null){
                product.setCategory1Id(categoryView.getCategory1Id());
                product.setCategory1Name(categoryView.getCategory1Name());
                product.setCategory2Id(categoryView.getCategory2Id());
                product.setCategory2Name(categoryView.getCategory2Name());
                product.setCategory3Id(categoryView.getCategory3Id());
                product.setCategory3Name(categoryView.getCategory3Name());
            }
            //商品的平台属性信息
            List<PlatformPropertyKey> propertyKeyList = productFeignClient.getPlatformPropertyBySkuId(skuId);
            if (!CollectionUtils.isEmpty(propertyKeyList)){
                List<SearchPlatformProperty> platformPropertyList = propertyKeyList.stream().map(platformPropertyKey -> {
                    SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                    //平台属性id
                    searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                    //平台属性名称
                    searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());
                    //平台属性值
                    searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
                    return searchPlatformProperty;
                }).collect(Collectors.toList());
                product.setPlatformProperty(platformPropertyList);
            }
        }
        productRepository.save(product);
    }

    @SneakyThrows//可以解决异常信息,避免try catch影响代码美观
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //构建生成DSL语句
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //实现DSL语句的调用
        SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //结果分析
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //设置其他参数信息,总页数
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        boolean flag= searchResponseVo.getTotal() % searchParam.getPageSize()==0;
        long totalPage = 0;
        //对flag进行判断
        if (flag){
            totalPage =searchResponseVo.getTotal()/searchParam.getPageSize();
        }else {
            totalPage = searchResponseVo.getTotal()/searchParam.getPageSize() + 1;
        }
        searchResponseVo.setTotalPages(totalPage);
        return searchResponseVo;
    }

    //用户点击详情时,热点分数加一
    @Override
    public void incrHotScore(Long skuId) {
        //由于es数据存储在磁盘中,高并发操作数据磁盘效率较慢,所以先将数据存入redis中,当达到一定数量时再同步到es中
        //给redis一个标题
        String key = "sku:hotScore";
        Double count = redisTemplate.opsForZSet().incrementScore(key, skuId, 1);
        if (count % 2 == 0){
            Optional<Product> optional = productRepository.findById(skuId);
            Product product = optional.get();
            product.setHotScore(Math.round(count));
            productRepository.save(product);
        }
    }

    @Override
    public void offSale(Long skuId) {
        productRepository.deleteById(skuId);
    }

    //结果分析
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //商品的基本信息集合
        SearchHits firstHits = searchResponse.getHits();
        //获取总记录数
        long totalHits = firstHits.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        SearchHit[] secondHits = firstHits.getHits();
        if (secondHits != null && secondHits.length >0){
            for (SearchHit secondHit : secondHits) {
                //将jSON解析为对象,对象存储数据JSONObject.parseObject
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null){
                    Text fragment = highlightField.getFragments()[0];
                    product.setProductName(fragment.toString());
                }
                //把单个的product信息添加到返回vo对象的list里面
                searchResponseVo.getProductList().add(product);
            }
        }
        //2.品牌聚合信息
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        List<SearchBrandVo> brandVoList = buckets.stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //获取品牌id
            searchBrandVo.setBrandId(bucket.getKeyAsNumber().longValue());
            //获取品牌name
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //获取品牌logo存放地址
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String logoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(logoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(brandVoList);
        //3.平台属性聚合信息
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyList = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
            // 平台属性Id
            long propertyKeyId = bucket.getKeyAsNumber().longValue();
            searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId);
            //当前属性值的集合
            ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
            String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
            searchPlatformPropertyVo.setPropertyKey(propertyKey);
            //属性名称
            ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
            List<String> propertyValueList = propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
            return searchPlatformPropertyVo;
        }).collect(Collectors.toList());
        searchResponseVo.setPlatformPropertyList(platformPropertyList);
        return searchResponseVo;
    }

    //构建生成Dsl
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //0.先创建一个大括号
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //1.构建bool字段
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //2.构建分类过滤器,对搜索数中传入的id进行判断防止空指针
        //构建一级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        //构建二级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        //构建三级分类过滤器
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //3.构造一个品牌过滤器 1:苹果
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)){
            String[] brandParam = brandName.split(":");
            //切割后需要判断数组长度是否等于2
            if (brandParam.length == 2){
                firstBool.filter(QueryBuilders.termQuery("brandId",brandParam[0]));
            }
        }
        //4.关键字查询must
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)){
            MatchQueryBuilder productName = QueryBuilders.matchQuery("productName", searchParam.getKeyword()).operator(Operator.AND);
            firstBool.filter(productName);
        }
        //5.构造平台属性过滤器
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0){
            for (String prop : props) {
                String[] platFormParams = prop.split(":");
                //切割后要判断切割后的长度是否符合
                if (platFormParams.length == 3){
                    //构造第二个bool
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    //获取到must下的第一个参数
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platFormParams[0]));
                    //获取到must下的第二个参数
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platFormParams[1]));
                    //none表示不做评分机制
                    NestedQueryBuilder platformProperty = QueryBuilders.nestedQuery("platformProperty", secondBool, ScoreMode.None);
                    firstBool.filter(platformProperty);
                }
            }
        }
        //把firstBool放入到query中
        searchSourceBuilder.query(firstBool);
        /*
        * 6.分页相关信息
        * */
        //获取Dsl中的from
        int from = (searchParam.getPageNo()-1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //获取Dsl中的size
        searchSourceBuilder.size(searchParam.getPageSize());
        /*
        * 7.排序相关信息的设置
        * 综合排序 order=1 desc
        * 加个排序 price order = 2  desc
        * */
        String order = searchParam.getOrder();
        if (StringUtils.isEmpty(order)) {
            String[] orderParam = order.split(":");
            //判断分割后参数的长度不能为null  ==2
            if (orderParam.length == 2) {
                String fileName = "";
                String param = orderParam[0];
                switch (param) {
                    case "1":
                        fileName = "hotScore";
                        break;
                    case "2":
                        fileName = "price";
                        break;
                }
                searchSourceBuilder.sort(fileName, "asc".equals(orderParam[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }
        /*
        * 8.高亮显示
        * */
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        /*
        * 9聚合
        * 品牌聚合
        * 属性聚合
        * */
        //品牌聚合
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        searchSourceBuilder.aggregation(aggregationBuilder);

        //属性聚合(嵌套的模式要注意括号)
        searchSourceBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg","platformProperty")
                .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));
        /*
        * 10让他知道是查询哪一个index和type
        * */
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        System.out.println("拼接好的Dsl语句" + searchSourceBuilder.toString());
        return searchRequest;
    }
}
