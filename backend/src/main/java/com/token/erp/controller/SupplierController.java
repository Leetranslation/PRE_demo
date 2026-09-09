package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Supplier;
import com.token.erp.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/supplier")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:supplier')")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/list")
    public Result<PageResult<Supplier>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(supplierService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:supplier:create')")
    public Result<Supplier> create(@RequestBody Supplier supplier) {
        return Result.success(supplierService.create(supplier));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:supplier:update')")
    public Result<Supplier> update(@RequestBody Supplier supplier) {
        return Result.success(supplierService.update(supplier));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:supplier:update')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        supplierService.changeStatus(id, status);
        return Result.success();
    }
}
