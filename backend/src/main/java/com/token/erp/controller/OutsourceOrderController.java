package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.OutsourceOrderRequest;
import com.token.erp.entity.OutsourceOrder;
import com.token.erp.entity.OutsourceOrderItem;
import com.token.erp.service.OutsourceOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/outsource/order")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('outsource:order')")
public class OutsourceOrderController {

    private final OutsourceOrderService outsourceOrderService;

    @GetMapping("/list")
    public Result<PageResult<OutsourceOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(outsourceOrderService.page(page, pageSize, orderId, type, approveStatus)));
    }

    @GetMapping("/items")
    public Result<List<OutsourceOrderItem>> items(@RequestParam Long outsourceId) {
        return Result.success(outsourceOrderService.items(outsourceId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('outsource:order:create')")
    public Result<OutsourceOrder> create(@RequestBody OutsourceOrderRequest req) {
        return Result.success(outsourceOrderService.create(req));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('outsource:order:create')")
    public Result<OutsourceOrder> update(@RequestBody OutsourceOrderRequest req) {
        return Result.success(outsourceOrderService.update(req));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('outsource:order:create')")
    public Result<Void> submit(@RequestParam Long id) {
        outsourceOrderService.submit(id);
        return Result.success();
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('outsource:order:approve')")
    public Result<Void> approve(@RequestParam Long id) {
        outsourceOrderService.approve(id);
        return Result.success();
    }

    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('outsource:order:approve')")
    public Result<Void> unApprove(@RequestParam Long id) {
        outsourceOrderService.unApprove(id);
        return Result.success();
    }

    @PostMapping("/start")
    @PreAuthorize("hasAuthority('outsource:order:start')")
    public Result<Void> start(@RequestParam Long id) {
        outsourceOrderService.start(id);
        return Result.success();
    }

    @PostMapping("/finish")
    @PreAuthorize("hasAuthority('outsource:order:start')")
    public Result<Void> finish(@RequestParam Long id) {
        outsourceOrderService.finish(id);
        return Result.success();
    }

    @PostMapping("/void")
    @PreAuthorize("hasAuthority('outsource:order:start')")
    public Result<Void> voidOrder(@RequestParam Long id) {
        outsourceOrderService.voidOrder(id);
        return Result.success();
    }
}
