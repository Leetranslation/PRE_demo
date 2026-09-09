package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_invoice")
@Schema(description = "销售发票")
public class SalesInvoice extends BaseEntity {
    @Schema(description = "发票号（FP+日期+流水）")
    private String invoiceNo;
    @Schema(description = "销售订单ID")
    private Long orderId;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "开票日期")
    private LocalDate invoiceDate;
    @Schema(description = "开票金额")
    private BigDecimal amount;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "业务状态：pending/partial/verified")
    private String bizStatus;
    @Schema(description = "已核销金额")
    private BigDecimal verifiedAmount;
    @Schema(description = "备注")
    private String remark;
}
