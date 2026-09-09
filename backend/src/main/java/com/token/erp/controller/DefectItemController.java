package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.DefectItem;
import com.token.erp.service.DefectItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/defect")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:defect')")
public class DefectItemController {

    private final DefectItemService defectItemService;

    @GetMapping("/list")
    public Result<PageResult<DefectItem>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(defectItemService.page(page, pageSize, keyword, category, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:defect:create')")
    public Result<DefectItem> create(@RequestBody DefectItem di) {
        return Result.success(defectItemService.create(di));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:defect:create')")
    public Result<DefectItem> update(@RequestBody DefectItem di) {
        return Result.success(defectItemService.update(di));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:defect:create')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        defectItemService.status(id, status);
        return Result.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('base:defect:create')")
    public Result<Void> delete(@RequestParam Long id) {
        defectItemService.delete(id);
        return Result.success();
    }
}
