package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.MaterialSubstitute;
import com.token.erp.service.MaterialSubstituteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/substitute")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:substitute')")
public class MaterialSubstituteController {

    private final MaterialSubstituteService substituteService;

    @GetMapping("/list")
    public Result<PageResult<MaterialSubstitute>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(substituteService.page(page, pageSize, materialId, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:substitute:create')")
    public Result<MaterialSubstitute> create(@RequestBody MaterialSubstitute ms) {
        return Result.success(substituteService.create(ms));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:substitute:create')")
    public Result<MaterialSubstitute> update(@RequestBody MaterialSubstitute ms) {
        return Result.success(substituteService.update(ms));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:substitute:create')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        substituteService.status(id, status);
        return Result.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('base:substitute:create')")
    public Result<Void> delete(@RequestParam Long id) {
        substituteService.delete(id);
        return Result.success();
    }
}
