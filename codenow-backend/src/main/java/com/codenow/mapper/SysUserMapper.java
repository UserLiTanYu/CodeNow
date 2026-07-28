package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问接口。
 * <p>
 * 提供对系统用户表（sys_user）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 存储用户账号、密码、角色、状态等核心信息。
 * </p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
