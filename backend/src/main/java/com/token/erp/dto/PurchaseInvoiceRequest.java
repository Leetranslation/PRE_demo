package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "采购发票请求")
public class PurchaseInvoiceRequest {
    @Schema(description = "发票ID（更新时）")
    private Long id;
    @Schema(description = "采购订单ID")
    private Long orderId;
    @Schema(description = "开票日期")
    private LocalDate invoiceDate;
    @Schema(description = "发票金额")
    private BigDecimal amount;
    @Schema(description = "备注")
    private String remark;
}
