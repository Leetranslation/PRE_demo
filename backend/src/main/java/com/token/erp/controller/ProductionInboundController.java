package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProductionInboundRequest;
import com.token.erp.entity.ProductionInbound;
import com.token.erp.service.ProductionInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/inbound")
@RequiredArgsConstructor
public class ProductionInboundController {

    private final ProductionInboundService inboundService;

    @GetMapping("/list")
    public Result<PageResult<ProductionInbound>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(inboundService.page(page, pageSize, orderId)));
    }

    @PostMapping("/create")
    public Result<ProductionInbound> create(@RequestBody ProductionInboundRequest request) {
        return Result.success(inboundService.create(request));
    }
}
