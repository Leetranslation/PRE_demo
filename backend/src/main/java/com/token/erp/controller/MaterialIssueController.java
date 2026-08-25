package com.token.erp.controller;

import com.token.erp.common.Result;
import com.token.erp.dto.MaterialIssueRequest;
import com.token.erp.entity.MaterialIssue;
import com.token.erp.service.MaterialIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/production/issue")
@RequiredArgsConstructor
public class MaterialIssueController {

    private final MaterialIssueService issueService;

    @PostMapping("/create")
    public Result<MaterialIssue> create(@RequestBody MaterialIssueRequest request) {
        return Result.success(issueService.create(request));
    }
}
