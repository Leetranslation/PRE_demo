package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_work")
@Schema(description = "报工单")
public class ReportWork extends BaseEntity {
    @Schema(description = "报工单号（BW+日期+流水）")
    private String reportNo;
    @Schema(description = "生产工单ID")
    private Long workOrderId;
    @Schema(description = "工序ID")
    private Long processId;
    @Schema(description = "工艺路线明细ID")
    private Long processRouteItemId;
    @Schema(description = "机台ID")
    private Long machineId;
    @Schema(description = "车间ID")
    private Long workshopId;
    @Schema(description = "报工人（用户ID）")
    private Long reporterId;
    @Schema(description = "报工人姓名")
    private String reporterName;
    @Schema(description = "报工时间")
    private LocalDateTime reportDate;
    @Schema(description = "报工数量")
    private BigDecimal quantity;
    @Schema(description = "良品数量")
    private BigDecimal qualifiedQty;
    @Schema(description = "不良品数量")
    private BigDecimal defectiveQty;
    @Schema(description = "工时（小时）")
    private BigDecimal workHours;
    @Schema(description = "工序开始时间")
    private LocalDateTime startTime;
    @Schema(description = "工序结束时间")
    private LocalDateTime endTime;
    @Schema(description = "审工状态：pending待审/approved已审/rejected驳回")
    private String auditStatus;
    @Schema(description = "审核人")
    private Long auditorId;
    @Schema(description = "审核人姓名")
    private String auditorName;
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
    @Schema(description = "审核意见")
    private String auditRemark;
    @Schema(description = "备注")
    private String remark;
}
