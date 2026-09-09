package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.BomRequest;
import com.token.erp.entity.Bom;
import com.token.erp.entity.BomItem;
import com.token.erp.entity.BomVersion;
import com.token.erp.service.BomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/bom")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:bom')")
public class BomController {

    private final BomService bomService;

    @GetMapping("/list")
    public Result<PageResult<Bom>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(bomService.page(page, pageSize, keyword, status)));
    }

    @GetMapping("/items")
    public Result<List<BomItem>> items(@RequestParam Long bomId) {
        return Result.success(bomService.items(bomId));
    }

    @GetMapping("/versions")
    public Result<List<BomVersion>> versions(@RequestParam Long bomId) {
        return Result.success(bomService.versions(bomId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:bom:create')")
    public Result<Bom> create(@RequestBody BomRequest request) {
        return Result.success(bomService.create(request));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:bom:update')")
    public Result<Bom> update(@RequestBody BomRequest request) {
        return Result.success(bomService.update(request));
    }
}
