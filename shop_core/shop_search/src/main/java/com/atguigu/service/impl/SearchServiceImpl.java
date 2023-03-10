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
        //?????????????????????
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo!=null){
            product.setId(skuInfo.getId());
            product.setProductName(skuInfo.getSkuName());
            product.setCreateTime(new Date());
            product.setPrice(skuInfo.getPrice().doubleValue());
            product.setDefaultImage(skuInfo.getSkuDefaultImg());
            //????????????
            Long brandId = skuInfo.getBrandId();
            BaseBrand baseBrand = productFeignClient.getBrandById(brandId);
            if (baseBrand!=null) {
                product.setBrandId(brandId);
                product.setBrandLogoUrl(baseBrand.getBrandLogoUrl());
                product.setBrandName(baseBrand.getBrandName());
            }
            //?????????????????????
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
            //???????????????????????????
            List<PlatformPropertyKey> propertyKeyList = productFeignClient.getPlatformPropertyBySkuId(skuId);
            if (!CollectionUtils.isEmpty(propertyKeyList)){
                List<SearchPlatformProperty> platformPropertyList = propertyKeyList.stream().map(platformPropertyKey -> {
                    SearchPlatformProperty searchPlatformProperty = new SearchPlatformProperty();
                    //????????????id
                    searchPlatformProperty.setPropertyKeyId(platformPropertyKey.getId());
                    //??????????????????
                    searchPlatformProperty.setPropertyKey(platformPropertyKey.getPropertyKey());
                    //???????????????
                    searchPlatformProperty.setPropertyValue(platformPropertyKey.getPropertyValueList().get(0).getPropertyValue());
                    return searchPlatformProperty;
                }).collect(Collectors.toList());
                product.setPlatformProperty(platformPropertyList);
            }
        }
        productRepository.save(product);
    }

    @SneakyThrows//????????????????????????,??????try catch??????????????????
    @Override
    public SearchResponseVo searchProduct(SearchParam searchParam) {
        //????????????DSL??????
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        //??????DSL???????????????
        SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //????????????
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);
        //????????????????????????,?????????
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        boolean flag= searchResponseVo.getTotal() % searchParam.getPageSize()==0;
        long totalPage = 0;
        //???flag????????????
        if (flag){
            totalPage =searchResponseVo.getTotal()/searchParam.getPageSize();
        }else {
            totalPage = searchResponseVo.getTotal()/searchParam.getPageSize() + 1;
        }
        searchResponseVo.setTotalPages(totalPage);
        return searchResponseVo;
    }

    //?????????????????????,??????????????????
    @Override
    public void incrHotScore(Long skuId) {
        //??????es????????????????????????,???????????????????????????????????????,????????????????????????redis???,????????????????????????????????????es???
        //???redis????????????
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

    //????????????
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //???????????????????????????
        SearchHits firstHits = searchResponse.getHits();
        //??????????????????
        long totalHits = firstHits.getTotalHits();
        searchResponseVo.setTotal(totalHits);
        SearchHit[] secondHits = firstHits.getHits();
        if (secondHits != null && secondHits.length >0){
            for (SearchHit secondHit : secondHits) {
                //???jSON???????????????,??????????????????JSONObject.parseObject
                Product product = JSONObject.parseObject(secondHit.getSourceAsString(), Product.class);
                HighlightField highlightField = secondHit.getHighlightFields().get("productName");
                if (highlightField != null){
                    Text fragment = highlightField.getFragments()[0];
                    product.setProductName(fragment.toString());
                }
                //????????????product?????????????????????vo?????????list??????
                searchResponseVo.getProductList().add(product);
            }
        }
        //2.??????????????????
        ParsedLongTerms brandIdAgg = searchResponse.getAggregations().get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        List<SearchBrandVo> brandVoList = buckets.stream().map(bucket -> {
            SearchBrandVo searchBrandVo = new SearchBrandVo();
            //????????????id
            searchBrandVo.setBrandId(bucket.getKeyAsNumber().longValue());
            //????????????name
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandName(brandName);
            //????????????logo????????????
            ParsedStringTerms brandLogoUrlAgg = bucket.getAggregations().get("brandLogoUrlAgg");
            String logoUrl = brandLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchBrandVo.setBrandLogoUrl(logoUrl);
            return searchBrandVo;
        }).collect(Collectors.toList());
        searchResponseVo.setBrandVoList(brandVoList);
        //3.????????????????????????
        ParsedNested platformPropertyAgg = searchResponse.getAggregations().get("platformPropertyAgg");
        ParsedLongTerms propertyKeyIdAgg = platformPropertyAgg.getAggregations().get("propertyKeyIdAgg");
        List<SearchPlatformPropertyVo> platformPropertyList = propertyKeyIdAgg.getBuckets().stream().map(bucket -> {
            SearchPlatformPropertyVo searchPlatformPropertyVo = new SearchPlatformPropertyVo();
            // ????????????Id
            long propertyKeyId = bucket.getKeyAsNumber().longValue();
            searchPlatformPropertyVo.setPropertyKeyId(propertyKeyId);
            //????????????????????????
            ParsedStringTerms propertyKeyAgg = bucket.getAggregations().get("propertyKeyAgg");
            String propertyKey = propertyKeyAgg.getBuckets().get(0).getKeyAsString();
            searchPlatformPropertyVo.setPropertyKey(propertyKey);
            //????????????
            ParsedStringTerms propertyValueAgg = bucket.getAggregations().get("propertyValueAgg");
            List<String> propertyValueList = propertyValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchPlatformPropertyVo.setPropertyValueList(propertyValueList);
            return searchPlatformPropertyVo;
        }).collect(Collectors.toList());
        searchResponseVo.setPlatformPropertyList(platformPropertyList);
        return searchResponseVo;
    }

    //????????????Dsl
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //0.????????????????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //1.??????bool??????
        BoolQueryBuilder firstBool = QueryBuilders.boolQuery();
        //2.?????????????????????,????????????????????????id???????????????????????????
        //???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            firstBool.filter(category1Id);
        }
        //???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            firstBool.filter(category2Id);
        }
        //???????????????????????????
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            firstBool.filter(category3Id);
        }
        //3.??????????????????????????? 1:??????
        String brandName = searchParam.getBrandName();
        if (!StringUtils.isEmpty(brandName)){
            String[] brandParam = brandName.split(":");
            //?????????????????????????????????????????????2
            if (brandParam.length == 2){
                firstBool.filter(QueryBuilders.termQuery("brandId",brandParam[0]));
            }
        }
        //4.???????????????must
        String keyword = searchParam.getKeyword();
        if (!StringUtils.isEmpty(keyword)){
            MatchQueryBuilder productName = QueryBuilders.matchQuery("productName", searchParam.getKeyword()).operator(Operator.AND);
            firstBool.filter(productName);
        }
        //5.???????????????????????????
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0){
            for (String prop : props) {
                String[] platFormParams = prop.split(":");
                //????????????????????????????????????????????????
                if (platFormParams.length == 3){
                    //???????????????bool
                    BoolQueryBuilder secondBool = QueryBuilders.boolQuery();
                    //?????????must?????????????????????
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyKeyId", platFormParams[0]));
                    //?????????must?????????????????????
                    secondBool.must(QueryBuilders.termQuery("platformProperty.propertyValue", platFormParams[1]));
                    //none????????????????????????
                    NestedQueryBuilder platformProperty = QueryBuilders.nestedQuery("platformProperty", secondBool, ScoreMode.None);
                    firstBool.filter(platformProperty);
                }
            }
        }
        //???firstBool?????????query???
        searchSourceBuilder.query(firstBool);
        /*
        * 6.??????????????????
        * */
        //??????Dsl??????from
        int from = (searchParam.getPageNo()-1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //??????Dsl??????size
        searchSourceBuilder.size(searchParam.getPageSize());
        /*
        * 7.???????????????????????????
        * ???????????? order=1 desc
        * ???????????? price order = 2  desc
        * */
        String order = searchParam.getOrder();
        if (StringUtils.isEmpty(order)) {
            String[] orderParam = order.split(":");
            //???????????????????????????????????????null  ==2
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
        * 8.????????????
        * */
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("productName");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);
        /*
        * 9??????
        * ????????????
        * ????????????
        * */
        //????????????
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("brandLogoUrlAgg").field("brandLogoUrl"));
        searchSourceBuilder.aggregation(aggregationBuilder);

        //????????????(??????????????????????????????)
        searchSourceBuilder.aggregation(AggregationBuilders.nested("platformPropertyAgg","platformProperty")
                .subAggregation(AggregationBuilders.terms("propertyKeyIdAgg").field("platformProperty.propertyKeyId")
                .subAggregation(AggregationBuilders.terms("propertyKeyAgg").field("platformProperty.propertyKey"))
                .subAggregation(AggregationBuilders.terms("propertyValueAgg").field("platformProperty.propertyValue"))));
        /*
        * 10??????????????????????????????index???type
        * */
        SearchRequest searchRequest = new SearchRequest("product");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        System.out.println("????????????Dsl??????" + searchSourceBuilder.toString());
        return searchRequest;
    }
}
