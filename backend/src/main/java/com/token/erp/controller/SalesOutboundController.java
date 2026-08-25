package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.SalesOutboundRequest;
import com.token.erp.dto.SalesReturnRequest;
import com.token.erp.entity.SalesOutbound;
import com.token.erp.service.SalesOutboundService;
import com.token.erp.service.SalesReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundService outboundService;
    private final SalesReturnService returnService;

    @GetMapping("/outbound/list")
    public Result<PageResult<SalesOutbound>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(outboundService.page(page, pageSize, orderId)));
    }

    @PostMapping("/outbound/create")
    public Result<SalesOutbound> create(@RequestBody SalesOutboundRequest request) {
        return Result.success(outboundService.create(request));
    }

    @PostMapping("/return/create")
    public Result<Void> createReturn(@RequestBody SalesReturnRequest request) {
        returnService.create(request);
        return Result.success();
    }
}
