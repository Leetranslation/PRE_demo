package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseReturnRequest;
import com.token.erp.entity.PurchaseReturn;
import com.token.erp.entity.PurchaseReturnItem;
import com.token.erp.service.PurchaseReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "采购退货", description = "采购退货管理")
@RestController
@RequestMapping("/api/purchase/return")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:return')")
public class PurchaseReturnController {

    private final PurchaseReturnService returnService;

    @Operation(summary = "分页查询退货单")
    @GetMapping("/list")
    public Result<PageResult<PurchaseReturn>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(returnService.page(page, pageSize, orderId)));
    }

    @Operation(summary = "查询退货明细")
    @GetMapping("/items")
    public Result<List<PurchaseReturnItem>> items(@RequestParam Long returnId) {
        return Result.success(returnService.items(returnId));
    }

    @Operation(summary = "新增采购退货（库存出库）")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:return:create')")
    public Result<PurchaseReturn> create(@RequestBody PurchaseReturnRequest req) {
        return Result.success(returnService.create(req));
    }
}
