package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.MpsPlanRequest;
import com.token.erp.entity.MpsPlan;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.service.MpsPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "主生产计划", description = "主生产计划的登记与生成工单")
@RestController
@RequestMapping("/api/production/mps")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:mps')")
public class MpsPlanController {

    private final MpsPlanService mpsPlanService;

    @Operation(summary = "分页查询主生产计划")
    @GetMapping("/list")
    public Result<PageResult<MpsPlan>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(mpsPlanService.page(page, pageSize, status)));
    }

    @Operation(summary = "新增主生产计划")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:mps:create')")
    public Result<MpsPlan> create(@RequestBody MpsPlanRequest req) {
        return Result.success(mpsPlanService.create(req));
    }

    @Operation(summary = "生成工单")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('production:mps:create')")
    public Result<ProductionOrder> generate(@Parameter(description = "主生产计划ID") @RequestParam Long id) {
        return Result.success(mpsPlanService.generateWorkOrder(id));
    }
}
