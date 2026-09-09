package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.SalesOrderRequest;
import com.token.erp.entity.SalesOrder;
import com.token.erp.entity.SalesOrderItem;
import com.token.erp.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/order")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:order')")
public class SalesOrderController {

    private final SalesOrderService orderService;

    @GetMapping("/list")
    public Result<PageResult<SalesOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(orderService.page(page, pageSize, keyword, approveStatus)));
    }

    @GetMapping("/items")
    public Result<List<SalesOrderItem>> items(@RequestParam Long orderId) {
        return Result.success(orderService.items(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:order:create')")
    public Result<SalesOrder> create(@RequestBody SalesOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('sales:order:submit')")
    public Result<Void> submit(@RequestParam Long id) {
        orderService.submit(id);
        return Result.success();
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('sales:order:approve')")
    public Result<Void> approve(@RequestParam Long id) {
        orderService.approve(id);
        return Result.success();
    }

    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('sales:order:approve')")
    public Result<Void> unApprove(@RequestParam Long id) {
        orderService.unApprove(id);
        return Result.success();
    }
}
