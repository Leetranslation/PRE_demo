package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.InspectionMethod;
import com.token.erp.entity.InspectionScheme;
import com.token.erp.service.QualityBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quality/base")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('quality:base')")
public class QualityBaseController {

    private final QualityBaseService qualityBaseService;

    // ===== 检验方法 =====
    @GetMapping("/method/list")
    public Result<PageResult<InspectionMethod>> methodList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(qualityBaseService.methodPage(page, pageSize, keyword, status)));
    }

    @PostMapping("/method/create")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<InspectionMethod> methodCreate(@RequestBody InspectionMethod m) {
        return Result.success(qualityBaseService.methodCreate(m));
    }

    @PostMapping("/method/update")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<InspectionMethod> methodUpdate(@RequestBody InspectionMethod m) {
        return Result.success(qualityBaseService.methodUpdate(m));
    }

    @PostMapping("/method/status")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<Void> methodStatus(@RequestParam Long id, @RequestParam Integer status) {
        qualityBaseService.methodStatus(id, status);
        return Result.success();
    }

    // ===== 检验方案 =====
    @GetMapping("/scheme/list")
    public Result<PageResult<InspectionScheme>> schemeList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(qualityBaseService.schemePage(page, pageSize, keyword, itemType, status)));
    }

    @PostMapping("/scheme/create")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<InspectionScheme> schemeCreate(@RequestBody InspectionScheme s) {
        return Result.success(qualityBaseService.schemeCreate(s));
    }

    @PostMapping("/scheme/update")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<InspectionScheme> schemeUpdate(@RequestBody InspectionScheme s) {
        return Result.success(qualityBaseService.schemeUpdate(s));
    }

    @PostMapping("/scheme/status")
    @PreAuthorize("hasAuthority('quality:base:create')")
    public Result<Void> schemeStatus(@RequestParam Long id, @RequestParam Integer status) {
        qualityBaseService.schemeStatus(id, status);
        return Result.success();
    }
}
