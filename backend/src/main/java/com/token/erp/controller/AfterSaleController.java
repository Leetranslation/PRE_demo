package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.AfterSale;
import com.token.erp.service.AfterSaleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "售后管理", description = "售后服务管理")
@RestController
@RequestMapping("/api/sales/afterSale")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:afterSale')")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    @Operation(summary = "分页查询售后单")
    @GetMapping("/list")
    public Result<PageResult<AfterSale>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(afterSaleService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增售后单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:afterSale:create')")
    public Result<AfterSale> create(@RequestBody AfterSale afterSale) {
        return Result.success(afterSaleService.create(afterSale));
    }

    @Operation(summary = "更新售后状态")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('sales:afterSale:create')")
    public Result<Void> status(@RequestParam Long id, @RequestParam String status) {
        afterSaleService.changeStatus(id, status);
        return Result.success();
    }
}
