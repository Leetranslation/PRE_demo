package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.MaterialReturnRequest;
import com.token.erp.entity.MaterialReturn;
import com.token.erp.entity.MaterialReturnItem;
import com.token.erp.service.MaterialReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production/return")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:return')")
public class MaterialReturnController {

    private final MaterialReturnService returnService;

    @GetMapping("/list")
    public Result<PageResult<MaterialReturn>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(returnService.page(page, pageSize, orderId)));
    }

    @GetMapping("/items")
    public Result<List<MaterialReturnItem>> items(@RequestParam Long returnId) {
        return Result.success(returnService.items(returnId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:return:create')")
    public Result<MaterialReturn> create(@RequestBody MaterialReturnRequest request) {
        return Result.success(returnService.create(request));
    }
}
