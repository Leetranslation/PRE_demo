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
    private String spec;
    private Integer inventoryManage;
    private String batchManage;
    private Integer productionQc;
    private Integer productionLeadDays;
    private BigDecimal minStock;
    private BigDecimal safetyStock;
    private BigDecimal maxStock;
    private Integer status;
    private String remark;
}
