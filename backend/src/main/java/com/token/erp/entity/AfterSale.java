package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("after_sale")
@Schema(description = "售后管理")
public class AfterSale extends BaseEntity {
    @Schema(description = "售后单号（SH+日期+流水）")
    private String afterSaleNo;
    @Schema(description = "销售订单ID")
    private Long orderId;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "售后类型：退货/维修/换货")
    private String type;
    @Schema(description = "状态：pending待处理/processing处理中/finished已完结")
    private String status;
    @Schema(description = "备注")
    private String remark;

    /** 销售订单号（冗余展示字段） */
    @TableField(exist = false)
    private String sourceOrderNo;
}
