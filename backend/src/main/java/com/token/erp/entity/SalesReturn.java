package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_return")
@Schema(description = "销售退货")
public class SalesReturn extends BaseEntity {
    @Schema(description = "退货单号（TH+日期+流水）")
    private String returnNo;
    @Schema(description = "销售订单ID")
    private Long orderId;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "入库仓库ID")
    private Long warehouseId;
    @Schema(description = "退货日期")
    private LocalDate returnDate;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "备注")
    private String remark;

    /** 销售订单号（冗余展示字段） */
    @TableField(exist = false)
    private String sourceOrderNo;
}
