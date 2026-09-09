package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_stocking")
@Schema(description = "销售备货单")
public class SalesStocking extends BaseEntity {
    @Schema(description = "备货单号（BH+日期+流水）")
    private String stockingNo;
    @Schema(description = "销售订单ID（可选）")
    private Long orderId;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "备注")
    private String remark;
}
