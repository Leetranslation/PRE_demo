package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ReportWorkRequest;
import com.token.erp.entity.ReportWork;
import com.token.erp.service.ReportWorkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "报工管理", description = "生产报工的登记与审工")
@RestController
@RequestMapping("/api/production/reportWork")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:reportWork')")
public class ReportWorkController {

    private final ReportWorkService reportWorkService;

    @Operation(summary = "分页查询报工单")
    @GetMapping("/list")
    public Result<PageResult<ReportWork>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "工单ID") @RequestParam(required = false) Long workOrderId,
            @Parameter(description = "审工状态：pending/approved/rejected") @RequestParam(required = false) String auditStatus) {
        return Result.success(PageResult.of(reportWorkService.page(page, pageSize, workOrderId, auditStatus)));
    }

    @Operation(summary = "新增报工")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:reportWork:create')")
    public Result<ReportWork> create(@RequestBody ReportWorkRequest req) {
        return Result.success(reportWorkService.create(req));
    }

    @Operation(summary = "审工通过")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('production:reportWork:audit')")
    public Result<Void> approve(@Parameter(description = "报工单ID") @RequestParam Long id) {
        reportWorkService.approve(id);
        return Result.success();
    }

    @Operation(summary = "审工反审核")
    @PostMapping("/unapprove")
    @PreAuthorize("hasAuthority('production:reportWork:audit')")
    public Result<Void> unApprove(@Parameter(description = "报工单ID") @RequestParam Long id) {
        reportWorkService.unApprove(id);
        return Result.success();
    }

    @Operation(summary = "审工驳回")
    @PostMapping("/reject")
    @PreAuthorize("hasAuthority('production:reportWork:audit')")
    public Result<Void> reject(@Parameter(description = "报工单ID") @RequestParam Long id,
                               @Parameter(description = "驳回意见") @RequestParam(required = false) String remark) {
        reportWorkService.reject(id, remark);
        return Result.success();
    }

    @Operation(summary = "一键审核（批量审核待审报工）")
    @PostMapping("/batchApprove")
    @PreAuthorize("hasAuthority('production:reportWork:audit')")
    public Result<Void> batchApprove(@Parameter(description = "工单ID（可选）") @RequestParam(required = false) Long workOrderId) {
        reportWorkService.batchApprove(workOrderId);
        return Result.success();
    }

    @Operation(summary = "按工单查工序进度")
    @GetMapping("/processProgress")
    public Result<java.util.List<com.token.erp.dto.ProcessProgressVO>> processProgress(
            @Parameter(description = "工单ID") @RequestParam Long workOrderId) {
        return Result.success(reportWorkService.processProgress(workOrderId));
    }
}
