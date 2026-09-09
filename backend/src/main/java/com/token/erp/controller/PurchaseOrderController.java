package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseOrderRequest;
import com.token.erp.entity.PurchaseOrder;
import com.token.erp.entity.PurchaseOrderItem;
import com.token.erp.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase/order")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:order')")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;

    @GetMapping("/list")
    public Result<PageResult<PurchaseOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(orderService.page(page, pageSize, keyword, approveStatus)));
    }

    @GetMapping("/items")
    public Result<List<PurchaseOrderItem>> items(@RequestParam Long orderId) {
        return Result.success(orderService.items(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:order:create')")
    public Result<PurchaseOrder> create(@RequestBody PurchaseOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('purchase:order:update')")
    public Result<PurchaseOrder> update(@RequestBody PurchaseOrderRequest request) {
        return Result.success(orderService.update(request));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('purchase:order:submit')")
    public Result<Void> submit(@RequestParam Long id) {
        orderService.submit(id);
        return Result.success();
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('purchase:order:approve')")
    public Result<Void> approve(@RequestParam Long id) {
        orderService.approve(id);
        return Result.success();
    }

    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('purchase:order:approve')")
    public Result<Void> unApprove(@RequestParam Long id) {
        orderService.unApprove(id);
        return Result.success();
    }
}
