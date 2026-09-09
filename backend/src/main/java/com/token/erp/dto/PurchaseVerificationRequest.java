package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "采购核销请求")
public class PurchaseVerificationRequest {
    @Schema(description = "核销单ID（更新时）")
    private Long id;
    @Schema(description = "采购发票ID")
    private Long invoiceId;
    @Schema(description = "核销金额（付款）")
    private BigDecimal amount;
    @Schema(description = "核销日期")
    private LocalDate verifyDate;
    @Schema(description = "备注")
    private String remark;
}
