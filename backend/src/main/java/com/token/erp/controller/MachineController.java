package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Machine;
import com.token.erp.service.MachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "机台设备", description = "机台设备的增删改查")
@RestController
@RequestMapping("/api/base/machine")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:machine')")
public class MachineController {

    private final MachineService machineService;

    @Operation(summary = "分页查询机台")
    @GetMapping("/list")
    public Result<PageResult<Machine>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status,
            @Parameter(description = "车间ID") @RequestParam(required = false) Long workshopId) {
        return Result.success(PageResult.of(machineService.page(page, pageSize, keyword, status, workshopId)));
    }

    @Operation(summary = "新增机台")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:machine:create')")
    public Result<Machine> create(@RequestBody Machine machine) {
        return Result.success(machineService.create(machine));
    }

    @Operation(summary = "编辑机台")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:machine:update')")
    public Result<Machine> update(@RequestBody Machine machine) {
        return Result.success(machineService.update(machine));
    }

    @Operation(summary = "启用/停用机台")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:machine:update')")
    public Result<Void> status(@Parameter(description = "机台ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        machineService.changeStatus(id, status);
        return Result.success();
    }
}
