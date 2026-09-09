package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.BeginBalance;
import com.token.erp.service.BeginBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/beginBalance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventory:beginBalance')")
public class BeginBalanceController {

    private final BeginBalanceService beginBalanceService;

    @GetMapping("/list")
    public Result<PageResult<BeginBalance>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(beginBalanceService.page(page, pageSize, targetType, keyword)));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('inventory:beginBalance:create')")
    public Result<BeginBalance> save(@RequestBody BeginBalance bb) {
        return Result.success(beginBalanceService.save(bb));
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('inventory:beginBalance:create')")
    public Result<Void> delete(@RequestParam Long id) {
        beginBalanceService.delete(id);
        return Result.success();
    }
}
