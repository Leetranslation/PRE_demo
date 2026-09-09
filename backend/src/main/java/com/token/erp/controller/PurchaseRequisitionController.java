package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.PurchaseRequisitionRequest;
import com.token.erp.entity.PurchaseRequisition;
import com.token.erp.entity.PurchaseRequisitionItem;
import com.token.erp.service.PurchaseRequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "采购申请单", description = "采购申请管理")
@RestController
@RequestMapping("/api/purchase/requisition")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('purchase:requisition')")
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService requisitionService;

    @Operation(summary = "分页查询采购申请单")
    @GetMapping("/list")
    public Result<PageResult<PurchaseRequisition>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（单号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(requisitionService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询申请单明细")
    @GetMapping("/items")
    public Result<List<PurchaseRequisitionItem>> items(@Parameter(description = "申请单ID") @RequestParam Long requisitionId) {
        return Result.success(requisitionService.items(requisitionId));
    }

    @Operation(summary = "新增采购申请单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('purchase:requisition:create')")
    public Result<PurchaseRequisition> create(@RequestBody PurchaseRequisitionRequest req) {
        return Result.success(requisitionService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('purchase:requisition:create')")
    public Result<Void> submit(@Parameter(description = "申请单ID") @RequestParam Long id) {
        requisitionService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('purchase:requisition:approve')")
    public Result<Void> approve(@Parameter(description = "申请单ID") @RequestParam Long id) {
        requisitionService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('purchase:requisition:approve')")
    public Result<Void> unApprove(@Parameter(description = "申请单ID") @RequestParam Long id) {
        requisitionService.unApprove(id);
        return Result.success();
    }
}
