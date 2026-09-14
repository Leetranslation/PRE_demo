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
@TableName("receivable")
@Schema(description = "应收台账")
public class Receivable extends BaseEntity {
    @Schema(description = "应收单号（YS+日期+流水）")
    private String receivableNo;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "销售发票ID")
    private Long invoiceId;
    @Schema(description = "发票号快照")
    private String invoiceNo;
    @Schema(description = "应收金额")
    private BigDecimal amount;
    @Schema(description = "已核销金额")
    private BigDecimal writtenOffAmount;
    @Schema(description = "未核销余额")
    private BigDecimal balance;
    @Schema(description = "状态：pending未结清/partial部分/settled已结清")
    private String status;
    @Schema(description = "备注")
    private String remark;

    /** 客户名称（冗余展示字段） */
    @TableField(exist = false)
    private String customerName;
}
