package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产工单创建/更新请求
 */
@Data
public class ProductionOrderRequest {
    private Long id;
    private Long productId;
    private BigDecimal quantity;
    private String productionType;
    private LocalDate planDate;
    private LocalDate planFinishDate;
    private Long departmentId;
    private Long workshopId;
    private Long machineId;
    private Long processRouteId;
    private String owner;
    private String urgency;
    private String outsourceStatus;
    private String sourceType;
    private Long sourceId;
    private String remark;
}
