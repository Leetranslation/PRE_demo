package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.MrpConfirmRequest;
import com.token.erp.dto.MrpRequest;
import com.token.erp.entity.MrpPlan;
import com.token.erp.entity.MrpRequirement;
import com.token.erp.service.MrpConfirmService;
import com.token.erp.service.MrpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mrp")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('mrp')")
public class MrpController {

    private final MrpService mrpService;
    private final MrpConfirmService confirmService;

    /** 执行 MRP 计算 */
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('mrp:calculate')")
    public Result<MrpPlan> calculate(@RequestBody MrpRequest request) {
        return Result.success(mrpService.calculate(request));
    }

    /** 计划列表 */
    @GetMapping("/plans")
    public Result<List<MrpPlan>> plans() {
        return Result.success(mrpService.plans());
    }

    /** 计划建议列表 */
    @GetMapping("/requirements")
    public Result<List<MrpRequirement>> requirements(@RequestParam Long planId) {
        return Result.success(mrpService.requirements(planId));
    }

    /** 确认并生成采购/生产单 */
    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('mrp:confirm')")
    public Result<Void> confirm(@RequestBody MrpConfirmRequest request) {
        confirmService.confirm(request);
        return Result.success();
    }
}
