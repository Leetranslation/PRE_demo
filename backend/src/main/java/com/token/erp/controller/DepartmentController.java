package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.entity.Department;
import com.token.erp.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/department")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:department')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/list")
    public Result<List<Department>> list() {
        return Result.success(departmentService.list());
    }

    @PostMapping("/create")
    public Result<Department> create(@RequestBody Department dept) {
        return Result.success(departmentService.create(dept));
    }

    @PostMapping("/update")
    public Result<Department> update(@RequestBody Department dept) {
        return Result.success(departmentService.update(dept));
    }
}
