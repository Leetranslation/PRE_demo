package com.token.erp.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.entity.Permission;
import com.token.erp.entity.Role;
import com.token.erp.entity.RolePermission;
import com.token.erp.entity.User;
import com.token.erp.entity.UserRole;
import com.token.erp.mapper.PermissionMapper;
import com.token.erp.mapper.RoleMapper;
import com.token.erp.mapper.RolePermissionMapper;
import com.token.erp.mapper.UserMapper;
import com.token.erp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        // 使用 Set 去重（用户可能有多个角色，权限码可能重复）
        Set<String> authoritySet = new LinkedHashSet<>();

        // 加载用户角色，角色 code 作为 ROLE_ 前缀 authority
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        for (UserRole ur : userRoles) {
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null && role.getStatus() != null && role.getStatus() == 1) {
                authoritySet.add("ROLE_" + role.getCode());
                // 加载该角色的权限码，作为 authority（用于 @PreAuthorize("hasAuthority(...)")）
                loadPermissions(role.getId(), authoritySet);
            }
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String auth : authoritySet) {
            authorities.add(new SimpleGrantedAuthority(auth));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus() != null && user.getStatus() == 1,
                true, true, true,
                authorities);
    }

    private void loadPermissions(Long roleId, Set<String> authoritySet) {
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        for (RolePermission rp : rps) {
            Permission perm = permissionMapper.selectById(rp.getPermissionId());
            if (perm != null && perm.getCode() != null) {
                authoritySet.add(perm.getCode());
            }
        }
    }
}
