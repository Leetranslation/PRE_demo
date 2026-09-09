package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_order")
public class ProductionOrder extends BaseEntity {
    private String orderNo;
    private String orderType;
    private Long productId;
    private BigDecimal quantity;
    private String productionType;
    private LocalDate planDate;
    private String sourceType;
    private Long sourceId;
    private LocalDate planFinishDate;
    private Long departmentId;
    private Long workshopId;
    private Long machineId;
    private Long processRouteId;
    private String owner;
    private Long bomId;
    private Long bomVersionId;
    private String approveStatus;
    private String bizStatus;
    private String finishStatus;
    private String inboundStatus;
    private String issueStatus;
    private String qcStatus;
    private String machineStatus;
    private String urgency;
    private String outsourceStatus;
    private BigDecimal completedQty;
    private String remark;
}
