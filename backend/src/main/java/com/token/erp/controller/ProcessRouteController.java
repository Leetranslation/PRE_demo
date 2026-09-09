package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.ProcessRouteRequest;
import com.token.erp.entity.ProcessRoute;
import com.token.erp.entity.ProcessRouteItem;
import com.token.erp.service.ProcessRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "工艺路线", description = "工艺路线的增删改查与工序明细管理")
@RestController
@RequestMapping("/api/base/processRoute")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:processRoute')")
public class ProcessRouteController {

    private final ProcessRouteService processRouteService;

    @Operation(summary = "分页查询工艺路线")
    @GetMapping("/list")
    public Result<PageResult<ProcessRoute>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字（名称/编号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(processRouteService.page(page, pageSize, keyword, status)));
    }

    @Operation(summary = "查询工艺路线明细")
    @GetMapping("/items")
    public Result<List<ProcessRouteItem>> items(
            @Parameter(description = "工艺路线ID") @RequestParam Long routeId) {
        return Result.success(processRouteService.items(routeId));
    }

    @Operation(summary = "新增工艺路线（含工序明细）")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:processRoute:create')")
    public Result<ProcessRoute> create(@RequestBody ProcessRouteRequest req) {
        return Result.success(processRouteService.create(req));
    }

    @Operation(summary = "编辑工艺路线（明细整体替换）")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:processRoute:update')")
    public Result<ProcessRoute> update(@RequestBody ProcessRouteRequest req) {
        return Result.success(processRouteService.update(req));
    }

    @Operation(summary = "启用/停用工艺路线")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:processRoute:update')")
    public Result<Void> status(
            @Parameter(description = "工艺路线ID") @RequestParam Long id,
            @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        processRouteService.changeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除工艺路线（级联删除明细）")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('base:processRoute:update')")
    public Result<Void> delete(@Parameter(description = "工艺路线ID") @RequestParam Long id) {
        processRouteService.delete(id);
        return Result.success();
    }
}
