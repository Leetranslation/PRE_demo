package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.EquipmentRepair;
import com.token.erp.service.EquipmentRepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备维修", description = "设备维修工单（报修/维修/完成）")
@RestController
@RequestMapping("/api/equipment/repair")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('equipment')")
public class EquipmentRepairController {

    private final EquipmentRepairService repairService;

    @Operation(summary = "分页查询维修工单")
    @GetMapping("/list")
    public Result<PageResult<EquipmentRepair>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) String repairStatus) {
        return Result.success(PageResult.of(repairService.page(page, pageSize, machineId, repairStatus)));
    }

    @Operation(summary = "报修（新增维修工单）")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentRepair> create(@RequestBody EquipmentRepair repair) {
        return Result.success(repairService.create(repair));
    }

    @Operation(summary = "编辑维修工单")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<EquipmentRepair> update(@RequestBody EquipmentRepair repair) {
        return Result.success(repairService.update(repair));
    }

    @Operation(summary = "更新维修状态（pending/repairing/done）")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> status(@RequestParam Long id, @RequestParam String status) {
        repairService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除维修工单")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> delete(@RequestParam Long id) {
        repairService.delete(id);
        return Result.success();
    }
}
