package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PackingRequest;
import com.token.erp.entity.Packing;
import com.token.erp.entity.PackingItem;
import com.token.erp.service.PackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "装箱单", description = "装箱管理")
@RestController
@RequestMapping("/api/sales/packing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:packing')")
public class PackingController {

    private final PackingService packingService;

    @Operation(summary = "分页查询装箱单")
    @GetMapping("/list")
    public Result<PageResult<Packing>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(packingService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询装箱明细")
    @GetMapping("/items")
    public Result<List<PackingItem>> items(@RequestParam Long packingId) {
        return Result.success(packingService.items(packingId));
    }

    @Operation(summary = "新增装箱单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:packing:create')")
    public Result<Packing> create(@RequestBody PackingRequest req) {
        return Result.success(packingService.create(req));
    }
}
