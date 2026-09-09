package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.entity.Permission;
import com.token.erp.entity.Role;
import com.token.erp.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:role')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.list());
    }

    @PostMapping("/create")
    public Result<Role> create(@RequestBody Role role) {
        return Result.success(roleService.create(role));
    }

    @PostMapping("/update")
    public Result<Role> update(@RequestBody Role role) {
        return Result.success(roleService.update(role));
    }

    @GetMapping("/permissions")
    public Result<List<Long>> permissions(@RequestParam Long roleId) {
        return Result.success(roleService.permissions(roleId));
    }

    @PostMapping("/assign")
    public Result<Void> assign(@RequestParam Long roleId, @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(roleId, permissionIds);
        return Result.success();
    }

    @GetMapping("/permission-list")
    public Result<List<Permission>> permissionList() {
        return Result.success(roleService.allPermissions());
    }
}
