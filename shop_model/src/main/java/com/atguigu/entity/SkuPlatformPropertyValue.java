package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * sku平台属性值关联表
 * </p>
 *
 * @author zhangqiang
 * @since 2021-03-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sku_platform_property_value")
@ApiModel(value="SkuPlatformPropertyValue对象", description="sku平台属性值关联表")
public class SkuPlatformPropertyValue implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "属性id（冗余)")
    private Long propertyKeyId;

    @ApiModelProperty(value = "属性值id")
    private Long propertyValueId;

    @ApiModelProperty(value = "skuid")
    private Long skuId;


}
