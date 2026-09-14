package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.EquipmentMaintenance;
import com.token.erp.service.EquipmentMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备保养", description = "设备保养计划与记录")
@RestController
@RequestMapping("/api/equipment/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('equipment')")
public class EquipmentMaintenanceController {

    private final EquipmentMaintenanceService maintenanceService;

    @Operation(summary = "分页查询保养记录")
    @GetMapping("/list")
    public Result<PageResult<EquipmentMaintenance>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(maintenanceService.page(page, pageSize, machineId, status)));
    }

    @Operation(summary = "新增保养计划")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentMaintenance> create(@RequestBody EquipmentMaintenance maintenance) {
        return Result.success(maintenanceService.create(maintenance));
    }

    @Operation(summary = "编辑保养记录")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentMaintenance> update(@RequestBody EquipmentMaintenance maintenance) {
        return Result.success(maintenanceService.update(maintenance));
    }

    @Operation(summary = "完成保养")
    @PostMapping("/complete")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> complete(@RequestParam Long id) {
        maintenanceService.complete(id);
        return Result.success();
    }

    @Operation(summary = "删除保养记录")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> delete(@RequestParam Long id) {
        maintenanceService.delete(id);
        return Result.success();
    }
}
