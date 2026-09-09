package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.SalesStockingRequest;
import com.token.erp.entity.SalesStocking;
import com.token.erp.entity.SalesStockingItem;
import com.token.erp.service.SalesStockingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "销售备货单", description = "销售备货管理")
@RestController
@RequestMapping("/api/sales/stocking")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:stocking')")
public class SalesStockingController {

    private final SalesStockingService stockingService;

    @Operation(summary = "分页查询备货单")
    @GetMapping("/list")
    public Result<PageResult<SalesStocking>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(stockingService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询备货明细")
    @GetMapping("/items")
    public Result<List<SalesStockingItem>> items(@RequestParam Long stockingId) {
        return Result.success(stockingService.items(stockingId));
    }

    @Operation(summary = "新增备货单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:stocking:create')")
    public Result<SalesStocking> create(@RequestBody SalesStockingRequest req) {
        return Result.success(stockingService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('sales:stocking:create')")
    public Result<Void> submit(@RequestParam Long id) {
        stockingService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('sales:stocking:create')")
    public Result<Void> approve(@RequestParam Long id) {
        stockingService.approve(id);
        return Result.success();
    }
}
