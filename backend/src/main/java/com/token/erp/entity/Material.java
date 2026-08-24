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
    private Integer status;
    private String remark;
}
