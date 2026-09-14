package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payable")
@Schema(description = "应付台账")
public class Payable extends BaseEntity {
    @Schema(description = "应付单号（YF+日期+流水）")
    private String payableNo;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "采购发票ID")
    private Long invoiceId;
    @Schema(description = "发票号快照")
    private String invoiceNo;
    @Schema(description = "应付金额")
    private BigDecimal amount;
    @Schema(description = "已核销金额")
    private BigDecimal writtenOffAmount;
    @Schema(description = "未核销余额")
    private BigDecimal balance;
    @Schema(description = "状态：pending未结清/partial部分/settled已结清")
    private String status;
    @Schema(description = "备注")
    private String remark;

    /** 供应商名称（冗余展示字段） */
    @TableField(exist = false)
    private String supplierName;
}
