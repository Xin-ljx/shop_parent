package com.atguigu.controller;

import com.atguigu.ProductFeignClient;
import com.atguigu.SearchFeignClient;
import com.atguigu.result.RetVal;
import com.atguigu.search.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebIndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @RequestMapping({"/","index.html"})
    public String index(Model model){
        //远程调用shop-product服务,拿到商品分类信息
        RetVal retVal = productFeignClient.gateIndexCategory();
        Object data = retVal.getData();
        model.addAttribute("list",data);
        return "index/index";
    }
    //搜索页面跳转
    @RequestMapping({"search.html"})
    public String search(SearchParam searchParam,Model model){
        //远程调用shop-search服务,拿到商品分类信息
        RetVal<Map> retVal = searchFeignClient.searchProduct(searchParam);
        //前端需要的是键值对要转换为Map,若不是Map则拿不到数据
        model.addAllAttributes(retVal.getData());
        //1.浏览器搜索路径参数进行链接'
        String urlParam = browserPageUrlParam(searchParam);
        model.addAttribute("urlParam",urlParam);
        //2.页面回显品牌信息brand
        String brandNameParam = pageBrandParam(searchParam.getBrandName());
        model.addAttribute("brandNameParam",brandNameParam);
        //3.页面回显平台属性信息platform
        List<Map<String, String>> propsParamList = pagePlatformParam(searchParam.getProps());
        model.addAttribute("propsParamList",propsParamList);
        //4.浏览器路径排序的拼接
        Map<String,Object> orderMap = pageSortParam(searchParam.getOrder());
        model.addAttribute("orderMap",orderMap);
        return "search/index";
    }
    //4.浏览器路径排序的拼接
    private Map<String, Object> pageSortParam(String order) {
        HashMap<String, Object> orderMap = new HashMap<>();
        if (!StringUtils.isEmpty(order)){
            String[] orderSplit = order.split(":");
            if (orderSplit.length == 2){
                orderMap.put("type",orderSplit[0]);
                orderMap.put("sort",orderSplit[1]);
            }
        }else{
            //给一个默认排序
            orderMap.put("type",1);
            orderMap.put("sort","desc");
        }
        return orderMap;
    }

    //3.页面回显平台属性信息platform
    private List<Map<String, String>> pagePlatformParam(String[] props) {
        List<Map<String, String>> propMapList= new ArrayList<>();
        if (props!=null&&props.length>0){
            for (String prop : props) {
                //props=4:苹果A14:CPU型号
                String[] platformSplit = prop.split(":");
                if (platformSplit.length == 3){
                    Map<String, String> map = new HashMap<>();
                    map.put("propertyKeyId",platformSplit[0]);
                    map.put("propertyKey",platformSplit[2]);
                    map.put("propertyValue",platformSplit[1]);
                    propMapList.add(map);
                }
            }

        }
        return propMapList;
    }

    //回显品牌信息
    //&brandName=1:苹果
    private String pageBrandParam(String brandName) {
       //判断brandName是否为空
        if (!StringUtils.isEmpty(brandName)){
            String[] brandSplit = brandName.split(":");
            if (brandSplit.length == 2){
                return "品牌" + brandSplit[1];
            }
        }
        return null;
    }



    //keyword=苹果智能&brandName=1:苹果&props=4:苹果A14:CPU型号&props=5:5.0英寸以下:屏幕尺寸
    private String browserPageUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断是否有关键字
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断是否有品牌名称
        if (!StringUtils.isEmpty(searchParam.getBrandName())){
            if (urlParam.length()>0) {
                urlParam.append("&brandName=").append(searchParam.getBrandName());
            }
        }
        //判断是否有品牌参数
        if (!StringUtils.isEmpty(searchParam.getProps())){
            if (urlParam.length()>0){
                //品牌参数可能是多个,所以需要遍历取出品牌参数再进行拼接
                for (String prop : searchParam.getProps()) {
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "search.html?"+urlParam.toString();
    }
}
