package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知数据访问接口。
 * <p>
 * 提供对用户通知表（user_notification）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 存储系统发送给用户的通知消息，支持已读/未读状态管理。
 * </p>
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {}
