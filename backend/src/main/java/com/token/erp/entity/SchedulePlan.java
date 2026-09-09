package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_plan")
@Schema(description = "生产排产计划")
public class SchedulePlan extends BaseEntity {
    @Schema(description = "排产单号（PC+日期+流水）")
    private String scheduleNo;
    @Schema(description = "生产工单ID")
    private Long workOrderId;
    @Schema(description = "机台ID")
    private Long machineId;
    @Schema(description = "车间ID")
    private Long workshopId;
    @Schema(description = "计划开始时间")
    private LocalDateTime planStart;
    @Schema(description = "计划结束时间")
    private LocalDateTime planEnd;
    @Schema(description = "状态：planned/started/finished/cancelled")
    private String status;
    @Schema(description = "备注")
    private String remark;
}
