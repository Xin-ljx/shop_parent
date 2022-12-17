package com.atguigu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.atguigu.dao.BaseCategoryViewMapper;
import com.atguigu.service.BaseCategoryViewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * VIEW 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-02
 */
@Service
public class BaseCategoryViewServiceImpl extends ServiceImpl<BaseCategoryViewMapper, BaseCategoryView> implements BaseCategoryViewService {
    @Override
    public List<JSONObject> getIndexCategory() {
        AtomicInteger index = new AtomicInteger(1);
        //查询所有商品分类信息
        List<BaseCategoryView> baseCategoryViews = baseMapper.selectList(null);
/*        for (Map.Entry<Long, List<BaseCategoryView>> longListEntry : category1Map.entrySet()) {

        }*/
        //查询一级分类index ,categoryId, categoryName, categoryChild[]
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        List<JSONObject> categoryList = category1Map.entrySet().stream().map(category1Entry -> {
            Long category1Id = category1Entry.getKey();
            List<BaseCategoryView> category1List = category1Entry.getValue();
            //创建一个JSON接收需要往前端返回的JSON数据
            JSONObject category1JSON = new JSONObject();
            category1JSON.put("index", index.getAndIncrement());
            category1JSON.put("categoryId", category1Id);
            category1JSON.put("categoryName", category1List.get(0).getCategory1Name());
            //查询二级分类categoryId,categoryName,categoryChild
            Map<Long, List<BaseCategoryView>> category2Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            List<JSONObject> category1Child = category2Map.entrySet().stream().map(category2Entry -> {
                Long category2Id = category2Entry.getKey();
                List<BaseCategoryView> category2List = category2Entry.getValue();
                JSONObject category2JSON = new JSONObject();
                category2JSON.put("categoryId", category2Id);
                category2JSON.put("categoryName", category2List.get(0).getCategory2Name());
                //查询三级分类categoryName,categoryId
                Map<Long, List<BaseCategoryView>> category3Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory3Id));
                List<JSONObject> category2Child = category3Map.entrySet().stream().map(category3Entry -> {
                    Long category3Id = category3Entry.getKey();
                    List<BaseCategoryView> category3List = category3Entry.getValue();
                    JSONObject category3JSON = new JSONObject();
                    category3JSON.put("categoryId", category3Id);
                    category3JSON.put("categoryName", category3List.get(0).getCategory3Name());
                    return category3JSON;
                }).collect(Collectors.toList());
                category2JSON.put("categoryChild", category2Child);
                //将前端需要的参数信息返回
                return category2JSON;
                //将收集到的信息转换为list封装在1级列表
            }).collect(Collectors.toList());
            category1JSON.put("categoryChild", category1Child);
            return category1JSON;
        }).collect(Collectors.toList());
        return categoryList;
    }
}