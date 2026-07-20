package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.SysUser;
import com.codenow.mapper.SysUserMapper;
import com.codenow.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 系统用户服务实现类。
 * 继承 MyBatis-Plus 通用服务，提供用户信息的 CRUD 基础能力。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}
