package com.token.erp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.MaterialIssueRequest;
import com.token.erp.entity.MaterialIssue;
import com.token.erp.entity.MaterialIssueItem;
import com.token.erp.service.MaterialIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production/issue")
@RequiredArgsConstructor
public class MaterialIssueController {

    private final MaterialIssueService issueService;

    @GetMapping("/list")
    public Result<PageResult<MaterialIssue>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId) {
        return Result.success(PageResult.of(issueService.page(page, pageSize, orderId)));
    }

    @GetMapping("/items")
    public Result<List<MaterialIssueItem>> items(@RequestParam Long issueId) {
        return Result.success(issueService.items(issueId));
    }

    @PostMapping("/create")
    public Result<MaterialIssue> create(@RequestBody MaterialIssueRequest request) {
        return Result.success(issueService.create(request));
    }
}
