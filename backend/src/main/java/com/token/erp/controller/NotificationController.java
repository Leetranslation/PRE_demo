package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.entity.Notification;
import com.token.erp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<List<Notification>> list(@RequestParam Long userId,
                                           @RequestParam(required = false) Integer isRead) {
        return Result.success(notificationService.list(userId, isRead));
    }

    @GetMapping("/unread")
    public Result<Long> unread(@RequestParam Long userId) {
        return Result.success(notificationService.unreadCount(userId));
    }

    @PostMapping("/read")
    public Result<Void> read(@RequestParam Long id, @RequestParam Long userId) {
        notificationService.markRead(id, userId);
        return Result.success();
    }
}
