package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Customer;
import com.token.erp.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/customer")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:customer')")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/list")
    public Result<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(customerService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:customer:create')")
    public Result<Customer> create(@RequestBody Customer customer) {
        return Result.success(customerService.create(customer));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:customer:update')")
    public Result<Customer> update(@RequestBody Customer customer) {
        return Result.success(customerService.update(customer));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:customer:update')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        customerService.changeStatus(id, status);
        return Result.success();
    }
}
