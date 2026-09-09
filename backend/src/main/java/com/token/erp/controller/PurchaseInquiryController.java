package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseInquiryRequest;
import com.token.erp.entity.PurchaseInquiry;
import com.token.erp.entity.PurchaseInquiryItem;
import com.token.erp.service.PurchaseInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "采购询价单", description = "采购询价管理")
@RestController
@RequestMapping("/api/purchase/inquiry")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:inquiry')")
public class PurchaseInquiryController {

    private final PurchaseInquiryService inquiryService;

    @Operation(summary = "分页查询询价单")
    @GetMapping("/list")
    public Result<PageResult<PurchaseInquiry>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（询价单号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(inquiryService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询询价单明细")
    @GetMapping("/items")
    public Result<List<PurchaseInquiryItem>> items(@Parameter(description = "询价单ID") @RequestParam Long inquiryId) {
        return Result.success(inquiryService.items(inquiryId));
    }

    @Operation(summary = "新增询价单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:inquiry:create')")
    public Result<PurchaseInquiry> create(@RequestBody PurchaseInquiryRequest req) {
        return Result.success(inquiryService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('purchase:inquiry:create')")
    public Result<Void> submit(@Parameter(description = "询价单ID") @RequestParam Long id) {
        inquiryService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('purchase:inquiry:approve')")
    public Result<Void> approve(@Parameter(description = "询价单ID") @RequestParam Long id) {
        inquiryService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('purchase:inquiry:approve')")
    public Result<Void> unApprove(@Parameter(description = "询价单ID") @RequestParam Long id) {
        inquiryService.unApprove(id);
        return Result.success();
    }
}
