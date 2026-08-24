package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Dictionary;
import com.token.erp.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/dict")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/list")
    public Result<PageResult<Dictionary>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(dictionaryService.page(page, pageSize, dictType, keyword)));
    }

    @GetMapping("/type")
    public Result<List<Dictionary>> byType(@RequestParam String dictType) {
        return Result.success(dictionaryService.listByType(dictType));
    }

    @PostMapping("/create")
    public Result<Dictionary> create(@RequestBody Dictionary dictionary) {
        return Result.success(dictionaryService.create(dictionary));
    }

    @PostMapping("/update")
    public Result<Dictionary> update(@RequestBody Dictionary dictionary) {
        return Result.success(dictionaryService.update(dictionary));
    }
}
