package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseVerificationRequest;
import com.token.erp.entity.PurchaseVerification;
import com.token.erp.service.PurchaseVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "采购核销", description = "采购付款核销")
@RestController
@RequestMapping("/api/purchase/verification")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:verification')")
public class PurchaseVerificationController {

    private final PurchaseVerificationService verificationService;

    @Operation(summary = "分页查询核销单")
    @GetMapping("/list")
    public Result<PageResult<PurchaseVerification>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(verificationService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "新增核销单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:verification:create')")
    public Result<PurchaseVerification> create(@RequestBody PurchaseVerificationRequest req) {
        return Result.success(verificationService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('purchase:verification:create')")
    public Result<Void> submit(@RequestParam Long id) {
        verificationService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('purchase:verification:approve')")
    public Result<Void> approve(@RequestParam Long id) {
        verificationService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('purchase:verification:approve')")
    public Result<Void> unApprove(@RequestParam Long id) {
        verificationService.unApprove(id);
        return Result.success();
    }
}
