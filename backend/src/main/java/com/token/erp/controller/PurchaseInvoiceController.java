package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseInvoiceRequest;
import com.token.erp.entity.PurchaseInvoice;
import com.token.erp.service.PurchaseInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "采购发票", description = "采购发票管理")
@RestController
@RequestMapping("/api/purchase/invoice")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:invoice')")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService invoiceService;

    @Operation(summary = "分页查询采购发票")
    @GetMapping("/list")
    public Result<PageResult<PurchaseInvoice>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（发票号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(invoiceService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "新增采购发票")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:invoice:create')")
    public Result<PurchaseInvoice> create(@RequestBody PurchaseInvoiceRequest req) {
        return Result.success(invoiceService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('purchase:invoice:create')")
    public Result<Void> submit(@Parameter(description = "发票ID") @RequestParam Long id) {
        invoiceService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('purchase:invoice:approve')")
    public Result<Void> approve(@Parameter(description = "发票ID") @RequestParam Long id) {
        invoiceService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('purchase:invoice:approve')")
    public Result<Void> unApprove(@Parameter(description = "发票ID") @RequestParam Long id) {
        invoiceService.unApprove(id);
        return Result.success();
    }
}
