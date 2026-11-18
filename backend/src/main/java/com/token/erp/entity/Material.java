package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
public class Material extends BaseEntity {
    private String code;
    private String name;
    private String category;
    private String unit;
    /** 单位ID（外键，unit 字段保留为冗余快照） */
    private Long unitId;
    private String spec;
    private String supplyType;
    private Integer inventoryManage;
    private Long defaultWarehouseId;
    private Long defaultLocationId;
    private BigDecimal safetyStock;
    private BigDecimal maxStock;
    private BigDecimal minStock;
    private String batchManage;
    private Integer purchaseQc;
    private Integer productionQc;
    private Integer purchaseLeadDays;
    /** 人工录入的平均生产周期（天/件）；为空则按历史生产工单计算 */
    private BigDecimal productionCycleDays;
    /** 人工录入的平均外协周期（整批天）；为空则按历史外协单据计算 */
    private BigDecimal outsourceCycleDays;
    private Integer status;
    /** 序列号规则ID（启用序列号时引用） */
    private Long serialRuleId;
    /** 标准成本单价（成本核算用） */
    private BigDecimal standardCost;
    private String remark;
}
