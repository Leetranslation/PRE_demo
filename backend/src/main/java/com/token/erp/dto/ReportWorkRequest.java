package com.token.erp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "报工请求参数")
public class ReportWorkRequest {
    @Schema(description = "报工单ID（更新时）")
    private Long id;
    @Schema(description = "生产工单ID")
    private Long workOrderId;
    @Schema(description = "工序ID")
    private Long processId;
    @Schema(description = "工艺路线明细ID")
    private Long processRouteItemId;
    @Schema(description = "机台ID")
    private Long machineId;
    @Schema(description = "报工数量")
    private BigDecimal quantity;
    @Schema(description = "良品数量")
    private BigDecimal qualifiedQty;
    @Schema(description = "不良品数量")
    private BigDecimal defectiveQty;
    @Schema(description = "工时（小时）")
    private BigDecimal workHours;
    @Schema(description = "工序开始时间（格式 yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @Schema(description = "工序结束时间（格式 yyyy-MM-dd HH:mm:ss）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @Schema(description = "备注")
    private String remark;
}
