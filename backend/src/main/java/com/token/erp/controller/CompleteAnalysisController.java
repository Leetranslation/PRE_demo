package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.MaterialShortageVO;
import com.token.erp.service.CompleteAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production/completeAnalysis")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:completeAnalysis')")
public class CompleteAnalysisController {

    private final CompleteAnalysisService completeAnalysisService;

    @GetMapping("/analysis")
    public Result<List<MaterialShortageVO>> analysis(@RequestParam Long workOrderId) {
        return Result.success(completeAnalysisService.analysis(workOrderId));
    }
}
