package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.token.erp.common.BizException;
import com.token.erp.entity.Permission;
import com.token.erp.entity.Role;
import com.token.erp.entity.RolePermission;
import com.token.erp.mapper.PermissionMapper;
import com.token.erp.mapper.RoleMapper;
import com.token.erp.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public List<Role> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    public Role create(Role role) {
        if (!StringUtils.hasText(role.getCode())) throw new BizException("角色编码不能为空");
        if (!StringUtils.hasText(role.getName())) throw new BizException("角色名称不能为空");
        role.setId(null);
        role.setDataScope(StringUtils.hasText(role.getDataScope()) ? role.getDataScope() : "self");
        role.setStatus(role.getStatus() == null ? 1 : role.getStatus());
        roleMapper.insert(role);
        return role;
    }

    public Role update(Role role) {
        if (roleMapper.selectById(role.getId()) == null) throw new BizException(404, "角色不存在");
        roleMapper.updateById(role);
        return roleMapper.selectById(role.getId());
    }

    /** 查询角色的权限ID列表 */
    public List<Long> permissions(Long roleId) {
        return rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId))
                .stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
    }

    /** 分配角色权限 */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) throw new BizException(404, "角色不存在");
        // 删除旧权限
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        // 添加新权限
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }

    /** 所有权限列表 */
    public List<Permission> allPermissions() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort).orderByAsc(Permission::getId));
    }
}
