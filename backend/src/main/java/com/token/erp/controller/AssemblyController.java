package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.AssemblyRequest;
import com.token.erp.entity.AssemblyOrder;
import com.token.erp.entity.AssemblyOrderItem;
import com.token.erp.service.AssemblyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/assembly")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory:assembly')")
public class AssemblyController {

    private final AssemblyService assemblyService;

    @GetMapping("/list")
    public Result<PageResult<AssemblyOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String type) {
        return Result.success(PageResult.of(assemblyService.page(page, pageSize, type)));
    }

    @GetMapping("/items")
    public Result<List<AssemblyOrderItem>> items(@RequestParam Long orderId) {
        return Result.success(assemblyService.items(orderId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('inventory:assembly:create')")
    public Result<AssemblyOrder> create(@RequestBody AssemblyRequest req) {
        return Result.success(assemblyService.create(req));
    }
}
