package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 用户地址表
 * </p>
 *
 * @author 尚硅谷张强
 * @since 2021-07-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户地址
     */
    private String userAddress;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 收件人
     */
    private String consignee;

    /**
     * 联系方式
     */
    private String phoneNum;

    /**
     * 是否是默认
     */
    private String isDefault;


}
