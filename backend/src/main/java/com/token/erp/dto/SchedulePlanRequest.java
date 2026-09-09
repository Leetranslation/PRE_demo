package com.token.erp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "排产计划请求")
public class SchedulePlanRequest {
    @Schema(description = "排产计划ID（更新时）")
    private Long id;
    @Schema(description = "生产工单ID")
    private Long workOrderId;
    @Schema(description = "机台ID")
    private Long machineId;
    @Schema(description = "车间ID")
    private Long workshopId;
    @Schema(description = "计划开始时间（格式 yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planStart;
    @Schema(description = "计划结束时间（格式 yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planEnd;
    @Schema(description = "备注")
    private String remark;
}
