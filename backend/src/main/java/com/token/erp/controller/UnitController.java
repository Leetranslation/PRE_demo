package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Unit;
import com.token.erp.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "单位管理", description = "单位的增删改查")
@RestController
@RequestMapping("/api/base/unit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:unit')")
public class UnitController {

    private final UnitService unitService;

    @Operation(summary = "分页查询单位")
    @GetMapping("/list")
    public Result<PageResult<Unit>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(unitService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增单位")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:unit:create')")
    public Result<Unit> create(@RequestBody Unit unit) {
        return Result.success(unitService.create(unit));
    }

    @Operation(summary = "编辑单位")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:unit:update')")
    public Result<Unit> update(@RequestBody Unit unit) {
        return Result.success(unitService.update(unit));
    }

    @Operation(summary = "启用/停用单位")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:unit:update')")
    public Result<Void> status(@Parameter(description = "单位ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        unitService.changeStatus(id, status);
        return Result.success();
    }
}
