package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Material;
import com.token.erp.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/material")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:material')")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping("/list")
    public Result<PageResult<Material>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(materialService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:material:create')")
    public Result<Material> create(@RequestBody Material material) {
        return Result.success(materialService.create(material));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:material:update')")
    public Result<Material> update(@RequestBody Material material) {
        return Result.success(materialService.update(material));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:material:update')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        materialService.changeStatus(id, status);
        return Result.success();
    }
}
