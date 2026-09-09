package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Area;
import com.token.erp.service.AreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "库区管理", description = "库区的增删改查")
@RestController
@RequestMapping("/api/base/area")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:area')")
public class AreaController {

    private final AreaService areaService;

    @Operation(summary = "分页查询库区")
    @GetMapping("/list")
    public Result<PageResult<Area>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        return Result.success(PageResult.of(areaService.page(page, pageSize, keyword, status, warehouseId)));
    }

    @Operation(summary = "新增库区")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:area:create')")
    public Result<Area> create(@RequestBody Area area) {
        return Result.success(areaService.create(area));
    }

    @Operation(summary = "编辑库区")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:area:update')")
    public Result<Area> update(@RequestBody Area area) {
        return Result.success(areaService.update(area));
    }

    @Operation(summary = "启用/停用库区")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:area:update')")
    public Result<Void> status(@Parameter(description = "库区ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        areaService.changeStatus(id, status);
        return Result.success();
    }
}
