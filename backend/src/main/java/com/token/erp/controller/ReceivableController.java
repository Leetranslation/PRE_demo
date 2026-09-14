package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Receivable;
import com.token.erp.service.ReceivableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/receivable")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finance')")
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping("/list")
    public Result<PageResult<Receivable>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(receivableService.page(page, pageSize, customerId, status)));
    }
}
