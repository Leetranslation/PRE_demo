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
@TableName("sales_verification")
@Schema(description = "销售核销")
public class SalesVerification extends BaseEntity {
    @Schema(description = "核销单号（HX+日期+流水）")
    private String verificationNo;
    @Schema(description = "销售发票ID")
    private Long invoiceId;
    @Schema(description = "销售订单ID")
    private Long orderId;
    @Schema(description = "核销金额（收款）")
    private BigDecimal amount;
    @Schema(description = "核销日期")
    private LocalDate verifyDate;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "备注")
    private String remark;

    /** 销售发票号（冗余展示字段） */
    @TableField(exist = false)
    private String invoiceNo;
}
