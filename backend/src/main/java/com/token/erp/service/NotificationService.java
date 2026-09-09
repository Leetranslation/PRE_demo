package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.Notification;
import com.token.erp.entity.User;
import com.token.erp.mapper.NotificationMapper;
import com.token.erp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知中心（文档 19.3 节）
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    /**
     * 发送通知（业务方调用）
     */
    public void send(Long userId, String type, String title, String content, String bizType, Long bizId) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    /**
     * 按用户名发送通知（内部转 userId，若用户不存在则忽略）
     */
    public void sendToUsername(String username, String type, String title, String content, String bizType, Long bizId) {
        if (username == null || username.isBlank()) return;
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return;
        send(user.getId(), type, title, content, bizType, bizId);
    }

    public List<Notification> list(Long userId, Integer isRead) {
        LambdaQueryWrapper<Notification> qw = new LambdaQueryWrapper<>();
        qw.eq(Notification::getUserId, userId);
        if (isRead != null) {
            qw.eq(Notification::getIsRead, isRead);
        }
        qw.orderByDesc(Notification::getId);
        return notificationMapper.selectList(qw);
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    public void markRead(Long id, Long userId) {
        Notification n = notificationMapper.selectById(id);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
        }
    }
}
