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
@TableName("receipt")
@Schema(description = "收款单")
public class Receipt extends BaseEntity {
    @Schema(description = "收款单号（SK+日期+流水）")
    private String receiptNo;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "收款金额")
    private BigDecimal amount;
    @Schema(description = "已核销金额")
    private BigDecimal writtenOffAmount;
    @Schema(description = "未核销余额")
    private BigDecimal balance;
    @Schema(description = "收款日期")
    private LocalDate receiptDate;
    @Schema(description = "收款账户")
    private String account;
    @Schema(description = "状态：pending未核销/partial部分/settled已核销完")
    private String status;
    @Schema(description = "备注")
    private String remark;

    /** 客户名称（冗余展示字段） */
    @TableField(exist = false)
    private String customerName;
}
