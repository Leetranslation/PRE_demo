package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.WorkCalendarRequest;
import com.token.erp.entity.WorkCalendar;
import com.token.erp.service.WorkCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "工作日历", description = "工作日历查询与批量设置")
@RestController
@RequestMapping("/api/base/workCalendar")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:workCalendar')")
public class WorkCalendarController {

    private final WorkCalendarService workCalendarService;

    @Operation(summary = "分页查询工作日历")
    @GetMapping("/list")
    public Result<PageResult<WorkCalendar>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "31") long pageSize,
            @Parameter(description = "月份（yyyy-MM）") @RequestParam(required = false) String yearMonth) {
        return Result.success(PageResult.of(workCalendarService.page(page, pageSize, yearMonth)));
    }

    @Operation(summary = "批量设置工作日/非工作日")
    @PostMapping("/batchSet")
    @PreAuthorize("hasAuthority('base:workCalendar:update')")
    public Result<Void> batchSet(@RequestBody WorkCalendarRequest req) {
        workCalendarService.batchSet(req);
        return Result.success();
    }
}
