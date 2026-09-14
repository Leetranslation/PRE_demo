package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.EquipmentCheck;
import com.token.erp.service.EquipmentCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备点检", description = "设备日常点检记录")
@RestController
@RequestMapping("/api/equipment/check")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('equipment')")
public class EquipmentCheckController {

    private final EquipmentCheckService checkService;

    @Operation(summary = "分页查询点检记录")
    @GetMapping("/list")
    public Result<PageResult<EquipmentCheck>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) String result) {
        return Result.success(PageResult.of(checkService.page(page, pageSize, machineId, result)));
    }

    @Operation(summary = "新增点检记录")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentCheck> create(@RequestBody EquipmentCheck check) {
        return Result.success(checkService.create(check));
    }

    @Operation(summary = "编辑点检记录")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentCheck> update(@RequestBody EquipmentCheck check) {
        return Result.success(checkService.update(check));
    }

    @Operation(summary = "删除点检记录")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> delete(@RequestParam Long id) {
        checkService.delete(id);
        return Result.success();
    }
}
