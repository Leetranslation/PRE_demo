package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.SalesOutboundRequest;
import com.token.erp.dto.SalesReturnRequest;
import com.token.erp.entity.SalesOutbound;
import com.token.erp.service.SalesOutboundService;
import com.token.erp.service.SalesReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundService outboundService;
    private final SalesReturnService returnService;

    @GetMapping("/outbound/list")
    @PreAuthorize("hasAuthority('sales:outbound')")
    public Result<PageResult<SalesOutbound>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(outboundService.page(page, pageSize, orderId)));
    }

    @GetMapping("/outbound/items")
    @PreAuthorize("hasAuthority('sales:outbound')")
    public Result<java.util.List<com.token.erp.entity.SalesOutboundItem>> items(@RequestParam Long outboundId) {
        return Result.success(outboundService.items(outboundId));
    }

    @PostMapping("/outbound/create")
    @PreAuthorize("hasAuthority('sales:outbound:create')")
    public Result<SalesOutbound> create(@RequestBody SalesOutboundRequest request) {
        return Result.success(outboundService.create(request));
    }

    @PostMapping("/return/create")
    @PreAuthorize("hasAuthority('sales:return:create')")
    public Result<Void> createReturn(@RequestBody SalesReturnRequest request) {
        returnService.create(request);
        return Result.success();
    }

    @GetMapping("/return/list")
    @PreAuthorize("hasAuthority('sales:return')")
    public Result<PageResult<com.token.erp.entity.SalesReturn>> returnList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(returnService.page(page, pageSize, orderId)));
    }

    @GetMapping("/return/items")
    @PreAuthorize("hasAuthority('sales:return')")
    public Result<java.util.List<com.token.erp.entity.SalesReturnItem>> returnItems(@RequestParam Long returnId) {
        return Result.success(returnService.items(returnId));
    }
}
