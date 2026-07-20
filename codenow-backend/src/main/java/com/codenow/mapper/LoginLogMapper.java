package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志数据访问接口。
 * <p>
 * 提供对登录日志表（login_log）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 记录用户登录的时间、IP 地址、设备信息及登录结果，用于安全审计。
 * </p>
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {}
