package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProductionOrderRequest;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.service.ProductionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/order")
@RequiredArgsConstructor
public class ProductionOrderController {

    private final ProductionOrderService orderService;

    @GetMapping("/list")
    public Result<PageResult<ProductionOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(orderService.page(page, pageSize, keyword, approveStatus)));
    }

    @PostMapping("/create")
    public Result<ProductionOrder> create(@RequestBody ProductionOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    @PostMapping("/submit")
    public Result<Void> submit(@RequestParam Long id) {
        orderService.submit(id);
        return Result.success();
    }

    @PostMapping("/approve")
    public Result<Void> approve(@RequestParam Long id) {
        orderService.approve(id);
        return Result.success();
    }

    @PostMapping("/unapprove")
    public Result<Void> unApprove(@RequestParam Long id) {
        orderService.unApprove(id);
        return Result.success();
    }
}
