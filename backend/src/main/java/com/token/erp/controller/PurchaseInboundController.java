package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseInboundRequest;
import com.token.erp.entity.PurchaseInbound;
import com.token.erp.service.PurchaseInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase/inbound")
@RequiredArgsConstructor
public class PurchaseInboundController {

    private final PurchaseInboundService inboundService;

    @GetMapping("/list")
    public Result<PageResult<PurchaseInbound>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(inboundService.page(page, pageSize, orderId)));
    }

    @PostMapping("/create")
    public Result<PurchaseInbound> create(@RequestBody PurchaseInboundRequest request) {
        return Result.success(inboundService.create(request));
    }
}
