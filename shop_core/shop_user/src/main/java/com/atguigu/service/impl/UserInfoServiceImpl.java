package com.atguigu.service.impl;

import com.atguigu.entity.UserInfo;
import com.atguigu.dao.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author lijiaxin
 * @since 2022-11-13
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Override
    public UserInfo queryUserFromDb(UserInfo uiUserInfo) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("login_name",uiUserInfo.getLoginName());
        String passwd = uiUserInfo.getPasswd();
        //对密码进行md5加密
        String encodedPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        queryWrapper.eq("passwd",encodedPasswd);
        return baseMapper.selectOne(queryWrapper);
    }
}
