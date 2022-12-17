package com.atguigu.service.impl;

import com.atguigu.entity.UserAddress;
import com.atguigu.dao.UserAddressMapper;
import com.atguigu.service.UserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户地址表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-13
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {

}
