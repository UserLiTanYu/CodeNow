package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.UserNotification;
import com.codenow.mapper.UserNotificationMapper;
import com.codenow.service.UserNotificationService;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification> implements UserNotificationService {}
