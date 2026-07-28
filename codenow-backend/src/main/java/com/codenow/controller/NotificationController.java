package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.entity.UserNotification;
import com.codenow.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member/notifications")
@RequiredArgsConstructor
/**
 * 用户通知控制器。
 * 提供通知列表查询、未读计数、标记已读等功能。
 */
public class NotificationController {
    private final UserNotificationService notificationService;

    /**
     * 分页查询当前用户的通知列表。
     * 未读通知优先显示。
     */
    @GetMapping
    public R<Page<UserNotification>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //分页查询当前用户的通知列表，未读通知优先显示，按创建时间倒序排列
        return R.ok(notificationService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .orderByAsc(UserNotification::getIsRead)
                        .orderByDesc(UserNotification::getCreateTime)));
    }

    /**
     * 查询当前用户未读通知数量。
     */
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        //查询当前用户的未读通知数量
        long count = notificationService.count(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(UserNotification::getIsRead, 0));
        //返回未读通知数量
        return R.ok(Map.of("count", count));
    }

    /**
     * 标记单条通知为已读。
     */
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        //根据通知ID和当前用户ID查询通知信息，确保只能操作自己的通知
        UserNotification notification = notificationService.getOne(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getId, id)
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong()));
        //通知不存在时返回404错误
        if (notification == null) return R.error(404, "通知不存在");
        //将通知标记为已读
        notification.setIsRead(1);
        notificationService.updateById(notification);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 标记所有通知为已读。
     */
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        //创建更新对象，设置为已读状态
        UserNotification update = new UserNotification();
        update.setIsRead(1);
        //批量更新当前用户所有未读通知为已读状态
        notificationService.update(update, new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(UserNotification::getIsRead, 0));
        //返回正确的响应结果
        return R.ok();
    }
}
