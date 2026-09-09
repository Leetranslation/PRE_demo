package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.UnitConversion;
import com.token.erp.service.UnitConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "多单位转换率", description = "单位换算率的增删改查")
@RestController
@RequestMapping("/api/base/unitConversion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:unitConversion')")
public class UnitConversionController {

    private final UnitConversionService unitConversionService;

    @Operation(summary = "分页查询换算率")
    @GetMapping("/list")
    public Result<PageResult<UnitConversion>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "状态：1启用 0停用") @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(unitConversionService.page(page, pageSize, status)));
    }

    @Operation(summary = "新增换算率")
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:unitConversion:create')")
    public Result<UnitConversion> create(@RequestBody UnitConversion conversion) {
        return Result.success(unitConversionService.create(conversion));
    }

    @Operation(summary = "编辑换算率")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:unitConversion:update')")
    public Result<UnitConversion> update(@RequestBody UnitConversion conversion) {
        return Result.success(unitConversionService.update(conversion));
    }

    @Operation(summary = "启用/停用换算率")
    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:unitConversion:update')")
    public Result<Void> status(@Parameter(description = "换算率ID") @RequestParam Long id,
                               @Parameter(description = "状态：1启用 0停用") @RequestParam Integer status) {
        unitConversionService.changeStatus(id, status);
        return Result.success();
    }
}
