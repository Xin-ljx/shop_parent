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
 * 基本销售属性表
 * </p>
 *
 * @author zhangqiang
 * @since 2021-03-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("base_sale_property")
@ApiModel(value="BaseSaleProperty对象", description="基本销售属性表")
public class BaseSaleProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "编号")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "销售属性名称")
    private String name;


}
