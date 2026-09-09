package com.token.erp.security;

import com.token.erp.common.BizException;
import com.token.erp.entity.User;
import com.token.erp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户工具
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserMapper userMapper;

    /** 当前登录用户名 */
    public String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BizException(401, "未登录");
        }
        return auth.getName();
    }

    /** 当前登录用户ID（从数据库按用户名查询） */
    public Long currentUserId() {
        String username = currentUsername();
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user.getId();
    }
}
