package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.DesignCenter;
import com.token.erp.service.DesignCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设计中心", description = "产品设计管理")
@RestController
@RequestMapping("/api/sales/designCenter")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:designCenter')")
public class DesignCenterController {

    private final DesignCenterService designCenterService;

    @Operation(summary = "分页查询设计单")
    @GetMapping("/list")
    public Result<PageResult<DesignCenter>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(designCenterService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增设计单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:designCenter:create')")
    public Result<DesignCenter> create(@RequestBody DesignCenter design) {
        return Result.success(designCenterService.create(design));
    }

    @Operation(summary = "更新设计状态")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('sales:designCenter:create')")
    public Result<Void> status(@RequestParam Long id, @RequestParam String status) {
        designCenterService.changeStatus(id, status);
        return Result.success();
    }
}
