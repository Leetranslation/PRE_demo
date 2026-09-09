package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Inventory;
import com.token.erp.entity.InventoryTransaction;
import com.token.erp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory')")
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
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String bizType) {
        return Result.success(PageResult.of(inventoryService.txPage(page, pageSize, itemId, itemType, bizType)));
    }

    @GetMapping("/warning")
    public Result<java.util.List<com.token.erp.dto.InventoryWarningVO>> warning() {
        return Result.success(inventoryService.warningList());
    }

    @GetMapping("/batchTrack")
    public Result<PageResult<InventoryTransaction>> batchTrack(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String batchNo) {
        return Result.success(PageResult.of(inventoryService.batchTrack(page, pageSize, batchNo)));
    }

    @GetMapping("/serialTrack")
    public Result<PageResult<InventoryTransaction>> serialTrack(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String serialNo) {
        return Result.success(PageResult.of(inventoryService.serialTrack(page, pageSize, serialNo)));
    }

    @GetMapping("/reservation/list")
    public Result<PageResult<com.token.erp.entity.InventoryReservation>> reservationList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(inventoryService.reservationPage(page, pageSize, status)));
    }

    @PostMapping("/reservation/lock")
    @PreAuthorize("hasAuthority('inventory:reserve')")
    public Result<com.token.erp.entity.InventoryReservation> lock(
            @RequestBody com.token.erp.dto.ReservationRequest req) {
        Inventory dim = new Inventory();
        dim.setItemId(req.getItemId());
        dim.setItemType(req.getItemType());
        dim.setWarehouseId(req.getWarehouseId());
        dim.setLocationId(req.getLocationId());
        dim.setBatchNo(req.getBatchNo());
        dim.setSerialNo(req.getSerialNo());
        dim.setStockType(req.getStockType() != null ? req.getStockType() : "available");
        return Result.success(inventoryService.reserve(dim, req.getQuantity(), req.getBizType(), req.getBizId(), req.getBizNo()));
    }

    @PostMapping("/reservation/release")
    @PreAuthorize("hasAuthority('inventory:reserve')")
    public Result<Void> release(@RequestParam Long reservationId) {
        inventoryService.release(reservationId);
        return Result.success();
    }
}
