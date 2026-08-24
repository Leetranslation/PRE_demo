package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.InventoryTransaction;
import com.token.erp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/list")
    public Result<PageResult<Inventory>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String stockType,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(
                inventoryService.page(page, pageSize, itemId, itemType, warehouseId, stockType, keyword)));
    }

    @GetMapping("/tx")
    public Result<PageResult<InventoryTransaction>> transactions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String bizType) {
        return Result.success(PageResult.of(inventoryService.txPage(page, pageSize, itemId, bizType)));
    }
}
