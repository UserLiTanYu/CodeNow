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
public class NotificationController {
    private final UserNotificationService notificationService;

    @GetMapping
    public R<Page<UserNotification>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(notificationService.page(new Page<>(pageNum, Math.min(pageSize, 50)),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .orderByAsc(UserNotification::getIsRead)
                        .orderByDesc(UserNotification::getCreateTime)));
    }

    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        long count = notificationService.count(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(UserNotification::getIsRead, 0));
        return R.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        UserNotification notification = notificationService.getOne(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getId, id)
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong()));
        if (notification == null) return R.error(404, "通知不存在");
        notification.setIsRead(1);
        notificationService.updateById(notification);
        return R.ok();
    }

    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        UserNotification update = new UserNotification();
        update.setIsRead(1);
        notificationService.update(update, new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, StpUtil.getLoginIdAsLong())
                .eq(UserNotification::getIsRead, 0));
        return R.ok();
    }
}
