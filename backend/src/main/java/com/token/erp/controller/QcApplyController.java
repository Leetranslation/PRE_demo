package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.QcApplyRequest;
import com.token.erp.entity.QcApply;
import com.token.erp.entity.QualityInspection;
import com.token.erp.service.QcApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "质检申请单", description = "质检申请（三段式第一段）+ 四线区分")
@RestController
@RequestMapping("/api/quality/apply")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('quality')")
public class QcApplyController {

    private final QcApplyService qcApplyService;

    @Operation(summary = "分页查询质检申请单")
    @GetMapping("/list")
    public Result<PageResult<QcApply>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "质检来源（四线）") @RequestParam(required = false) String sourceType,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(qcApplyService.page(page, pageSize, sourceType, status)));
    }

    @Operation(summary = "新增质检申请单")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('quality:create')")
    public Result<QcApply> create(@RequestBody QcApplyRequest req) {
        return Result.success(qcApplyService.create(req));
    }

    @Operation(summary = "申请单生成检验单")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('quality:create')")
    public Result<QualityInspection> generate(@Parameter(description = "申请单ID") @RequestParam Long applyId) {
        return Result.success(qcApplyService.generateInspection(applyId));
    }
}
