package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.QuotationRequest;
import com.token.erp.entity.Quotation;
import com.token.erp.entity.QuotationItem;
import com.token.erp.entity.SalesOrder;
import com.token.erp.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "报价单", description = "销售报价单管理")
@RestController
@RequestMapping("/api/sales/quotation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:quotation')")
public class QuotationController {

    private final QuotationService quotationService;

    @Operation(summary = "分页查询报价单")
    @GetMapping("/list")
    public Result<PageResult<Quotation>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（单号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(quotationService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询报价单明细")
    @GetMapping("/items")
    public Result<List<QuotationItem>> items(@Parameter(description = "报价单ID") @RequestParam Long quotationId) {
        return Result.success(quotationService.items(quotationId));
    }

    @Operation(summary = "新增报价单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:quotation:create')")
    public Result<Quotation> create(@RequestBody QuotationRequest req) {
        return Result.success(quotationService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('sales:quotation:create')")
    public Result<Void> submit(@Parameter(description = "报价单ID") @RequestParam Long id) {
        quotationService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('sales:quotation:approve')")
    public Result<Void> approve(@Parameter(description = "报价单ID") @RequestParam Long id) {
        quotationService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('sales:quotation:approve')")
    public Result<Void> unApprove(@Parameter(description = "报价单ID") @RequestParam Long id) {
        quotationService.unApprove(id);
        return Result.success();
    }

    @Operation(summary = "转销售订单")
    @PostMapping("/convert")
    @PreAuthorize("hasAuthority('sales:quotation:create')")
    public Result<SalesOrder> convert(@Parameter(description = "报价单ID") @RequestParam Long id) {
        return Result.success(quotationService.convertToSalesOrder(id));
    }
}
