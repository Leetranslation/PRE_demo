package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.CostResult;
import com.token.erp.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finance')")
public class CostController {

    private final CostService costService;

    @PostMapping("/calculate")
    public Result<CostResult> calculate(@RequestParam Long orderId) {
        return Result.success(costService.calculate(orderId));
    }
}
