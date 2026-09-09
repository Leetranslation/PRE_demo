package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.SerialRule;
import com.token.erp.service.SerialRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "序列号规则", description = "序列号生成规则的增删改查")
@RestController
@RequestMapping("/api/base/serialRule")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:serialRule')")
public class SerialRuleController {

    private final SerialRuleService serialRuleService;

    @Operation(summary = "分页查询序列号规则")
    @GetMapping("/list")
    public Result<PageResult<SerialRule>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(serialRuleService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增序列号规则")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:serialRule:create')")
    public Result<SerialRule> create(@RequestBody SerialRule rule) {
        return Result.success(serialRuleService.create(rule));
    }

    @Operation(summary = "编辑序列号规则")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:serialRule:update')")
    public Result<SerialRule> update(@RequestBody SerialRule rule) {
        return Result.success(serialRuleService.update(rule));
    }

    @Operation(summary = "启用/停用序列号规则")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:serialRule:update')")
    public Result<Void> status(@Parameter(description = "规则ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        serialRuleService.changeStatus(id, status);
        return Result.success();
    }
}
