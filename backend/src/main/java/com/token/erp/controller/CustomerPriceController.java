package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.CustomerPrice;
import com.token.erp.service.CustomerPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/base/customerPrice")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:customerPrice')")
public class CustomerPriceController {

    private final CustomerPriceService customerPriceService;

    @GetMapping("/list")
    public Result<PageResult<CustomerPrice>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(customerPriceService.page(page, pageSize, customerId, itemType, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:customerPrice:create')")
    public Result<CustomerPrice> create(@RequestBody CustomerPrice cp) {
        return Result.success(customerPriceService.create(cp));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:customerPrice:create')")
    public Result<CustomerPrice> update(@RequestBody CustomerPrice cp) {
        return Result.success(customerPriceService.update(cp));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('base:customerPrice:create')")
    public Result<Void> status(@RequestParam Long id, @RequestParam Integer status) {
        customerPriceService.status(id, status);
        return Result.success();
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('base:customerPrice:create')")
    public Result<Void> delete(@RequestParam Long id) {
        customerPriceService.delete(id);
        return Result.success();
    }
}
