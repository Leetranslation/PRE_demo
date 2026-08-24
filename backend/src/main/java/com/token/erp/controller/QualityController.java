package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.QualityInspection;
import com.token.erp.service.QualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/list")
    public Result<PageResult<QualityInspection>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String result) {
        return Result.success(PageResult.of(qualityService.page(page, pageSize, result)));
    }

    @PostMapping("/create")
    public Result<QualityInspection> create(@RequestBody QualityInspection inspection) {
        return Result.success(qualityService.create(inspection));
    }

    @PostMapping("/judge")
    public Result<Void> judge(@RequestParam Long inspectionId,
                              @RequestParam String result,
                              @RequestParam(required = false) String handleType,
                              @RequestParam(required = false) String reason) {
        qualityService.judge(inspectionId, result, handleType, reason);
        return Result.success();
    }
}
