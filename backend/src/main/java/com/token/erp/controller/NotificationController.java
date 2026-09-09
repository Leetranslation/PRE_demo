package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.entity.Notification;
import com.token.erp.security.SecurityUtils;
import com.token.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping("/list")
    public Result<List<Notification>> list(@RequestParam(required = false) Integer isRead) {
        Long userId = securityUtils.currentUserId();
        return Result.success(notificationService.list(userId, isRead));
    }

    @GetMapping("/unread")
    public Result<Long> unread() {
        Long userId = securityUtils.currentUserId();
        return Result.success(notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    public Result<Void> read(@RequestParam Long id) {
        Long userId = securityUtils.currentUserId();
        notificationService.markRead(id, userId);
        return Result.success();
    }
}
