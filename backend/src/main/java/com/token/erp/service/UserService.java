package com.token.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.BizException;
import com.token.erp.entity.User;
import com.token.erp.entity.UserRole;
import com.token.erp.mapper.UserMapper;
import com.token.erp.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<User> page(long page, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (status != null) qw.eq(User::getStatus, status);
        qw.orderByDesc(User::getId);
        return userMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Transactional
    public User create(User user) {
        if (!StringUtils.hasText(user.getUsername())) throw new BizException("用户名不能为空");
        validatePassword(user.getPassword());
        // 检查用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (count > 0) throw new BizException("用户名已存在");

        user.setId(null);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        userMapper.insert(user);
        return user;
    }

    public User update(User user) {
        if (userMapper.selectById(user.getId()) == null) throw new BizException(404, "用户不存在");
        // 密码不允许通过 update 修改（用专门的重置密码接口）
        user.setPassword(null);
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    /** 重置密码 */
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    /** 密码复杂度校验：长度至少 6 位 */
    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) throw new BizException("密码不能为空");
        if (password.length() < 6) throw new BizException("密码长度至少 6 位");
    }

    /** 启用/停用 */
    public void changeStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /** 查询用户的角色ID列表 */
    public List<Long> roles(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    /** 分配用户角色 */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userMapper.selectById(userId) == null) throw new BizException(404, "用户不存在");
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long rid : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }
    }
}
