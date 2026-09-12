package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.CockpitStats;
import com.token.erp.dto.DashboardStats;
import com.token.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStats> stats() {
        return Result.success(dashboardService.stats());
    }

    @GetMapping("/cockpit")
    public Result<CockpitStats> cockpit() {
        return Result.success(dashboardService.cockpit());
    }
}
