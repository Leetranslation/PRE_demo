package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.MaterialReturnRequest;
import com.token.erp.entity.MaterialReturn;
import com.token.erp.service.MaterialReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/return")
@RequiredArgsConstructor
public class MaterialReturnController {

    private final MaterialReturnService returnService;

    @PostMapping("/create")
    public Result<MaterialReturn> create(@RequestBody MaterialReturnRequest request) {
        return Result.success(returnService.create(request));
    }
}
