package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ContractRequest;
import com.token.erp.entity.Contract;
import com.token.erp.entity.ContractItem;
import com.token.erp.entity.SalesOrder;
import com.token.erp.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "合同管理", description = "销售合同管理")
@RestController
@RequestMapping("/api/sales/contract")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('sales:contract')")
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "分页查询合同")
    @GetMapping("/list")
    public Result<PageResult<Contract>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（合同号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "审批状态") @RequestParam(required = false) String approveStatus) {
        return Result.success(PageResult.of(contractService.page(page, pageSize, keyword, approveStatus)));
    }

    @Operation(summary = "查询合同明细")
    @GetMapping("/items")
    public Result<List<ContractItem>> items(@Parameter(description = "合同ID") @RequestParam Long contractId) {
        return Result.success(contractService.items(contractId));
    }

    @Operation(summary = "新增合同")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('sales:contract:create')")
    public Result<Contract> create(@RequestBody ContractRequest req) {
        return Result.success(contractService.create(req));
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('sales:contract:create')")
    public Result<Void> submit(@Parameter(description = "合同ID") @RequestParam Long id) {
        contractService.submit(id);
        return Result.success();
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('sales:contract:approve')")
    public Result<Void> approve(@Parameter(description = "合同ID") @RequestParam Long id) {
        contractService.approve(id);
        return Result.success();
    }

    @Operation(summary = "反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('sales:contract:approve')")
    public Result<Void> unApprove(@Parameter(description = "合同ID") @RequestParam Long id) {
        contractService.unApprove(id);
        return Result.success();
    }

    @Operation(summary = "转销售订单")
    @PostMapping("/convert")
    @PreAuthorize("hasAuthority('sales:contract:create')")
    public Result<SalesOrder> convert(@Parameter(description = "合同ID") @RequestParam Long id) {
        return Result.success(contractService.convertToSalesOrder(id));
    }
}
