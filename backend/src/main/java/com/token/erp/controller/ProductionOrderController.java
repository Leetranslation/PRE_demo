package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProductionOrderRequest;
import com.token.erp.entity.ProductionOrder;
import com.token.erp.service.ProductionOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/order")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:order')")
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
    @PreAuthorize("hasAuthority('production:order:create')")
    public Result<ProductionOrder> create(@RequestBody ProductionOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('production:order:submit')")
    public Result<Void> submit(@RequestParam Long id) {
        orderService.submit(id);
        return Result.success();
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('production:order:approve')")
    public Result<Void> approve(@RequestParam Long id) {
        orderService.approve(id);
        return Result.success();
    }

    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('production:order:approve')")
    public Result<Void> unApprove(@RequestParam Long id) {
        orderService.unApprove(id);
        return Result.success();
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('production:order:start')")
    public Result<Void> start(@RequestParam Long id) {
        orderService.start(id);
        return Result.success();
    }

    @PostMapping("/finish")
    @PreAuthorize("hasAuthority('production:order:start')")
    public Result<Void> finish(@RequestParam Long id) {
        orderService.finish(id);
        return Result.success();
    }

    @PostMapping("/void")
    @PreAuthorize("hasAuthority('production:order:start')")
    public Result<Void> voidOrder(@RequestParam Long id) {
        orderService.voidOrder(id);
        return Result.success();
    }
}
