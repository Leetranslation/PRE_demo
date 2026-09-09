package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.User;
import com.token.erp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:user')")
public class UserController {

    private final UserService userService;

    @GetMapping("/list")
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(userService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    public Result<User> create(@RequestBody User user) {
        return Result.success(userService.create(user));
    }

    @PostMapping("/update")
    public Result<User> update(@RequestBody User user) {
        return Result.success(userService.update(user));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PostMapping("/status")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return Result.success();
    }

    @GetMapping("/roles")
    public Result<List<Long>> roles(@RequestParam Long userId) {
        return Result.success(userService.roles(userId));
    }

    @PostMapping("/assign-roles")
    public Result<Void> assignRoles(@RequestParam Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return Result.success();
    }
}
