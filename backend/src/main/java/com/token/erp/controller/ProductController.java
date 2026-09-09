package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Product;
import com.token.erp.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/product")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:product')")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/list")
    public Result<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(productService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:product:create')")
    public Result<Product> create(@RequestBody Product product) {
        return Result.success(productService.create(product));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:product:update')")
    public Result<Product> update(@RequestBody Product product) {
        return Result.success(productService.update(product));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:product:update')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        productService.changeStatus(id, status);
        return Result.success();
    }
}
