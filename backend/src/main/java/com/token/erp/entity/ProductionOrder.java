package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 成本核算结果字段 */
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;
    private String remark;

    /** 产品编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
}
