package com.atguigu.service.impl;

import com.atguigu.entity.PlatformPropertyKey;
import com.atguigu.entity.PlatformPropertyValue;
import com.atguigu.dao.PlatformPropertyKeyMapper;
import com.atguigu.service.PlatformPropertyKeyService;
import com.atguigu.service.PlatformPropertyValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * <p>
 * 属性表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-10-30
 */
@Service
public class PlatformPropertyKeyServiceImpl extends ServiceImpl<PlatformPropertyKeyMapper, PlatformPropertyKey> implements PlatformPropertyKeyService {
    @Autowired
    private PlatformPropertyValueService propertyValueService;

    //获取商品属性列表信息
    @Override
    public List<PlatformPropertyKey> getPlatformPropertyByCategoryId(Long category1Id, Long category2Id, Long category3Id) {

        return baseMapper.getPlatformPropertyByCategoryId(category1Id,category2Id,category3Id);
    }

    //添加保存商品属性
    @Override
    @Transactional
    public void savePlatformProperty(PlatformPropertyKey platformPropertyKey) {
        //更新商品属性
        if (platformPropertyKey.getId()!=null){
            baseMapper.updateById(platformPropertyKey);
            //删除原来的信息
            QueryWrapper<PlatformPropertyValue> wrapper = new QueryWrapper<>();
            wrapper.eq("property_key_id",platformPropertyKey.getId());
            propertyValueService.remove(wrapper);
        }else{
            baseMapper.insert(platformPropertyKey);
        }
        //添加商品属性
        List<PlatformPropertyValue> propertyValueList = platformPropertyKey.getPropertyValueList();
        if (!CollectionUtils.isEmpty(propertyValueList)){
            for (PlatformPropertyValue platformPropertyValue : propertyValueList) {
                platformPropertyValue.setPropertyKeyId(platformPropertyKey.getId());
            }
            propertyValueService.saveBatch(propertyValueList);
        }
    }
}
