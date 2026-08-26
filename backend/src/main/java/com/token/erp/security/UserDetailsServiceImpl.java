package com.token.erp.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.Role;
import com.token.erp.entity.User;
import com.token.erp.entity.UserRole;
import com.token.erp.mapper.RoleMapper;
import com.token.erp.mapper.UserMapper;
import com.token.erp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        // 加载用户角色，作为 authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        for (UserRole ur : userRoles) {
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null && role.getStatus() != null && role.getStatus() == 1) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() != null && user.getStatus() == 1,
                true, true, true,
                authorities);
    }
}
