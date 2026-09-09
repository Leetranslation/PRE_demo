package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProductionInboundRequest;
import com.token.erp.entity.ProductionInbound;
import com.token.erp.service.ProductionInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/inbound")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:inbound')")
public class ProductionInboundController {

    private final ProductionInboundService inboundService;

    @GetMapping("/list")
    public Result<PageResult<ProductionInbound>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(inboundService.page(page, pageSize, orderId)));
    }

    @GetMapping("/items")
    public Result<java.util.List<com.token.erp.entity.ProductionInboundItem>> items(@RequestParam Long inboundId) {
        return Result.success(inboundService.items(inboundId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:inbound:create')")
    public Result<ProductionInbound> create(@RequestBody ProductionInboundRequest request) {
        return Result.success(inboundService.create(request));
    }
}
