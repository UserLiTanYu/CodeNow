package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.UserNotification;
import com.codenow.mapper.UserNotificationMapper;
import com.codenow.service.UserNotificationService;
import org.springframework.stereotype.Service;

/**
 * 用户通知服务实现类。
 * 管理站内通知消息的存储与查询，支持通知的已读/未读状态管理。
 */
@Service
public class UserNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification> implements UserNotificationService {}
