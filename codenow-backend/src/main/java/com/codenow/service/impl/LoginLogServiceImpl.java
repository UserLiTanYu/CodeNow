package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.LoginLog;
import com.codenow.mapper.LoginLogMapper;
import com.codenow.service.LoginLogService;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {}
