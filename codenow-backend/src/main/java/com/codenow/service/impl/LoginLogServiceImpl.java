package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.LoginLog;
import com.codenow.mapper.LoginLogMapper;
import com.codenow.service.LoginLogService;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现类。
 * 记录用户登录行为，包括登录时间、IP 地址、登录状态等信息，用于安全审计。
 */
@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {}
