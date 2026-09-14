package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Payable;
import com.token.erp.service.PayableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance/payable")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finance')")
public class PayableController {

    private final PayableService payableService;

    @GetMapping("/list")
    public Result<PageResult<Payable>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(payableService.page(page, pageSize, supplierId, status)));
    }
}
