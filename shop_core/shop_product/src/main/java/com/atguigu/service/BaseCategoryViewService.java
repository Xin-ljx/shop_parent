package com.atguigu.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.entity.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * VIEW 服务类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-02
 */
public interface BaseCategoryViewService extends IService<BaseCategoryView> {

    List<JSONObject> getIndexCategory();
}
