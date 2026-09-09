package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Stocktake;
import com.token.erp.entity.StocktakeItem;
import com.token.erp.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stocktake")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory:stocktake')")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @GetMapping("/list")
    public Result<PageResult<Stocktake>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(stocktakeService.page(page, pageSize, warehouseId, status)));
    }

    @GetMapping("/items")
    public Result<List<StocktakeItem>> items(@RequestParam Long stocktakeId) {
        return Result.success(stocktakeService.items(stocktakeId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('inventory:stocktake:create')")
    public Result<Stocktake> create(@RequestParam Long warehouseId,
                                    @RequestParam(required = false) String remark) {
        return Result.success(stocktakeService.create(warehouseId, remark));
    }

    @PostMapping("/count")
    @PreAuthorize("hasAuthority('inventory:stocktake:create')")
    public Result<Void> count(@RequestBody List<StocktakeItem> items) {
        stocktakeService.count(items);
        return Result.success();
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('inventory:stocktake:create')")
    public Result<Void> adjust(@RequestParam Long stocktakeId) {
        stocktakeService.adjust(stocktakeId);
        return Result.success();
    }
}
