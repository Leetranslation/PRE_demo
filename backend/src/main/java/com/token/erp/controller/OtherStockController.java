package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.OtherStockRequest;
import com.token.erp.entity.OtherStock;
import com.token.erp.entity.OtherStockItem;
import com.token.erp.service.OtherStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "其他出入库", description = "其他入库/出库单（盘盈盘亏/期初/领用等）")
@RestController
@RequestMapping("/api/inventory/otherStock")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory:otherStock')")
public class OtherStockController {

    private final OtherStockService stockService;

    @Operation(summary = "分页查询其他出入库单")
    @GetMapping("/list")
    public Result<PageResult<OtherStock>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "方向 in/out") @RequestParam(required = false) String direction,
            @Parameter(description = "类型") @RequestParam(required = false) String type) {
        return Result.success(PageResult.of(stockService.page(page, pageSize, direction, type)));
    }

    @Operation(summary = "查询明细")
    @GetMapping("/items")
    public Result<List<OtherStockItem>> items(@RequestParam Long stockId) {
        return Result.success(stockService.items(stockId));
    }

    @Operation(summary = "新增其他出入库单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('inventory:otherStock:create')")
    public Result<OtherStock> create(@RequestBody OtherStockRequest req) {
        return Result.success(stockService.create(req));
    }
}
