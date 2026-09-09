package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.SchedulePlanRequest;
import com.token.erp.entity.SchedulePlan;
import com.token.erp.service.SchedulePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "生产排产计划", description = "生产排产计划的登记与查询")
@RestController
@RequestMapping("/api/production/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:schedule')")
public class SchedulePlanController {

    private final SchedulePlanService schedulePlanService;

    @Operation(summary = "分页查询排产计划")
    @GetMapping("/list")
    public Result<PageResult<SchedulePlan>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "工单ID") @RequestParam(required = false) Long workOrderId,
            @Parameter(description = "机台ID") @RequestParam(required = false) Long machineId,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(schedulePlanService.page(page, pageSize, workOrderId, machineId, status)));
    }

    @Operation(summary = "新增排产计划")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:schedule:create')")
    public Result<SchedulePlan> create(@RequestBody SchedulePlanRequest req) {
        return Result.success(schedulePlanService.create(req));
    }

    @Operation(summary = "更新排产状态")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('production:schedule:create')")
    public Result<Void> status(@Parameter(description = "排产ID") @RequestParam Long id,
                               @Parameter(description = "状态") @RequestParam String status) {
        schedulePlanService.changeStatus(id, status);
        return Result.success();
    }
}
