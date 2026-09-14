package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Payment;
import com.token.erp.entity.PaymentWriteoff;
import com.token.erp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/payment")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finance')")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/list")
    public Result<PageResult<Payment>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(paymentService.page(page, pageSize, supplierId, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('finance:create')")
    public Result<Payment> create(@RequestBody Payment payment) {
        return Result.success(paymentService.create(payment));
    }

    @PostMapping("/writeoff")
    @PreAuthorize("hasAuthority('finance:writeoff')")
    public Result<Void> writeoff(@RequestParam Long paymentId, @RequestBody List<PaymentWriteoff> items) {
        paymentService.writeoff(paymentId, items);
        return Result.success();
    }

    @GetMapping("/writeoffItems")
    public Result<List<PaymentWriteoff>> writeoffItems(@RequestParam Long paymentId) {
        return Result.success(paymentService.writeoffItems(paymentId));
    }
}
