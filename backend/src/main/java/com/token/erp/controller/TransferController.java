package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.TransferRequest;
import com.token.erp.entity.Transfer;
import com.token.erp.entity.TransferItem;
import com.token.erp.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/transfer")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory:transfer')")
public class TransferController {

    private final TransferService transferService;

    @GetMapping("/list")
    public Result<PageResult<Transfer>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long sourceWarehouseId,
            @RequestParam(required = false) Long targetWarehouseId) {
        return Result.success(PageResult.of(
                transferService.page(page, pageSize, sourceWarehouseId, targetWarehouseId)));
    }

    @GetMapping("/items")
    public Result<List<TransferItem>> items(@RequestParam Long transferId) {
        return Result.success(transferService.items(transferId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('inventory:transfer:create')")
    public Result<Transfer> create(@RequestBody TransferRequest req) {
        return Result.success(transferService.create(req));
    }
}
