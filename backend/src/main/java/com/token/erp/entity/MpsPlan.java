package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mps_plan")
@Schema(description = "主生产计划")
public class MpsPlan extends BaseEntity {
    @Schema(description = "主生产计划单号（MP+日期+流水）")
    private String mpsNo;
    @Schema(description = "产品ID")
    private Long productId;
    @Schema(description = "计划数量")
    private BigDecimal quantity;
    @Schema(description = "计划生产日期")
    private LocalDate planDate;
    @Schema(description = "车间ID")
    private Long workshopId;
    @Schema(description = "状态：planned/confirmed")
    private String status;
    @Schema(description = "生成的工单ID")
    private Long workOrderId;
    @Schema(description = "备注")
    private String remark;
}
