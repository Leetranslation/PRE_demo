package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Receipt;
import com.token.erp.entity.ReceiptWriteoff;
import com.token.erp.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/receipt")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finance')")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/list")
    public Result<PageResult<Receipt>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(receiptService.page(page, pageSize, customerId, status)));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('finance:create')")
    public Result<Receipt> create(@RequestBody Receipt receipt) {
        return Result.success(receiptService.create(receipt));
    }

    @PostMapping("/writeoff")
    @PreAuthorize("hasAuthority('finance:writeoff')")
    public Result<Void> writeoff(@RequestParam Long receiptId, @RequestBody List<ReceiptWriteoff> items) {
        receiptService.writeoff(receiptId, items);
        return Result.success();
    }

    @GetMapping("/writeoffItems")
    public Result<List<ReceiptWriteoff>> writeoffItems(@RequestParam Long receiptId) {
        return Result.success(receiptService.writeoffItems(receiptId));
    }
}
