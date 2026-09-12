package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_invoice")
@Schema(description = "采购发票")
public class PurchaseInvoice extends BaseEntity {
    @Schema(description = "发票号（FP+日期+流水）")
    private String invoiceNo;
    @Schema(description = "采购订单ID")
    private Long orderId;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "开票日期")
    private LocalDate invoiceDate;
    @Schema(description = "发票金额")
    private BigDecimal amount;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "业务状态：pending/partial/verified")
    private String bizStatus;
    @Schema(description = "已核销金额")
    private BigDecimal verifiedAmount;
    @Schema(description = "备注")
    private String remark;

    /** 采购订单号（冗余展示字段） */
    @TableField(exist = false)
    private String sourceOrderNo;
}
