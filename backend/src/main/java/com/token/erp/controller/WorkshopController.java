package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Workshop;
import com.token.erp.service.WorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "车间管理", description = "车间基础数据的增删改查")
@RestController
@RequestMapping("/api/base/workshop")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:workshop')")
public class WorkshopController {

    private final WorkshopService workshopService;

    @Operation(summary = "分页查询车间")
    @GetMapping("/list")
    public Result<PageResult<Workshop>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(workshopService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "新增车间")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:workshop:create')")
    public Result<Workshop> create(@RequestBody Workshop workshop) {
        return Result.success(workshopService.create(workshop));
    }

    @Operation(summary = "编辑车间")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:workshop:update')")
    public Result<Workshop> update(@RequestBody Workshop workshop) {
        return Result.success(workshopService.update(workshop));
    }

    @Operation(summary = "启用/停用车间")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:workshop:update')")
    public Result<Void> status(@Parameter(description = "车间ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        workshopService.changeStatus(id, status);
        return Result.success();
    }
}
