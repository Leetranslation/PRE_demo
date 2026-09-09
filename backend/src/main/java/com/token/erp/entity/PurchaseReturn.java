package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_return")
@Schema(description = "采购退货")
public class PurchaseReturn extends BaseEntity {
    @Schema(description = "退货单号（TH+日期+流水）")
    private String returnNo;
    @Schema(description = "采购订单ID")
    private Long orderId;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "出库仓库ID")
    private Long warehouseId;
    @Schema(description = "退货日期")
    private LocalDate returnDate;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "备注")
    private String remark;
}
