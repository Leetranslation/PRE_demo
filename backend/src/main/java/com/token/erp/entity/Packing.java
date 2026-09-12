package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("packing")
@Schema(description = "装箱单")
public class Packing extends BaseEntity {
    @Schema(description = "装箱单号（ZX+日期+流水）")
    private String packingNo;
    @Schema(description = "销售出库单ID")
    private Long outboundId;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "备注")
    private String remark;

    /** 销售出库单号（冗余展示字段） */
    @TableField(exist = false)
    private String outboundNo;
}
