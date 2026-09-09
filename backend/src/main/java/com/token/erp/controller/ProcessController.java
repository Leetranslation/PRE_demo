package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProcessRequest;
import com.token.erp.entity.Process;
import com.token.erp.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工序管理", description = "工序基础数据的增删改查与批量设置")
@RestController
@RequestMapping("/api/base/process")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:process')")
public class ProcessController {

    private final ProcessService processService;

    @Operation(summary = "分页查询工序")
    @GetMapping("/list")
    public Result<PageResult<Process>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(processService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增工序")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:process:create')")
    public Result<Process> create(@RequestBody ProcessRequest req) {
        return Result.success(processService.create(req));
    }

    @Operation(summary = "编辑工序")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:process:update')")
    public Result<Process> update(@RequestBody ProcessRequest req) {
        return Result.success(processService.update(req));
    }

    @Operation(summary = "启用/停用工序")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:process:update')")
    public Result<Void> status(
            @Parameter(description = "工序ID") @RequestParam Long id,
            @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        processService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "批量设置标准单价")
    @PostMapping("/batchPrice")
    @PreAuthorize("hasAuthority('base:process:update')")
    public Result<Void> batchPrice(@RequestBody ProcessRequest req) {
        processService.batchSetPrice(req.getIds(), req.getPrice());
        return Result.success();
    }

    @Operation(summary = "批量设置计价方式")
    @PostMapping("/batchPriceType")
    @PreAuthorize("hasAuthority('base:process:update')")
    public Result<Void> batchPriceType(@RequestBody ProcessRequest req) {
        processService.batchSetPriceType(req.getIds(), req.getPriceTypeValue());
        return Result.success();
    }
}
