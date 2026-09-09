package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design_center")
@Schema(description = "设计中心")
public class DesignCenter extends BaseEntity {
    @Schema(description = "设计单号（SJ+日期+流水）")
    private String designNo;
    @Schema(description = "销售订单ID（可选）")
    private Long orderId;
    @Schema(description = "设计的产品ID")
    private Long productId;
    @Schema(description = "状态：designing设计中/finished已完成")
    private String status;
    @Schema(description = "审批状态")
    private String approveStatus;
    @Schema(description = "备注")
    private String remark;
}
