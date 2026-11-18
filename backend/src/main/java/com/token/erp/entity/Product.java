package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {
    private String code;
    private String name;
    private String category;
    private String unit;
    /** 单位ID（外键，unit 字段保留为冗余快照） */
    private Long unitId;
    private String spec;
    private Integer inventoryManage;
    private String batchManage;
    private Integer productionQc;
    private Integer productionLeadDays;
    /** 人工录入的平均采购周期（整批天）；为空则按历史采购单据计算 */
    private BigDecimal purchaseCycleDays;
    /** 人工录入的平均外协周期（整批天）；为空则按历史外协单据计算 */
    private BigDecimal outsourceCycleDays;
    private BigDecimal minStock;
    private BigDecimal safetyStock;
    private BigDecimal maxStock;
    /** 标准成本单价（成本核算用） */
    private BigDecimal standardCost;
    private Integer status;
    private String remark;
}
