package com.token.erp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.dto.MaterialIssueRequest;
import com.token.erp.entity.MaterialIssue;
import com.token.erp.entity.MaterialIssueItem;
import com.token.erp.service.MaterialIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production/issue")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('production:issue')")
public class MaterialIssueController {

    private final MaterialIssueService issueService;

    @GetMapping("/list")
    public Result<PageResult<MaterialIssue>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String issueType) {
        return Result.success(PageResult.of(issueService.page(page, pageSize, orderId, issueType)));
    }

    @GetMapping("/items")
    public Result<List<MaterialIssueItem>> items(@RequestParam Long issueId) {
        return Result.success(issueService.items(issueId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('production:issue:create')")
    public Result<MaterialIssue> create(@RequestBody MaterialIssueRequest request) {
        return Result.success(issueService.create(request));
    }
}
