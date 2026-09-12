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
@TableName("quality_inspection")
public class QualityInspection extends BaseEntity {
    private String inspectionNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long itemId;
    private String itemType;
    private Long warehouseId;
    private BigDecimal quantity;
    private LocalDate inspectionDate;
    private String result;
    private String remark;
    /** 库位ID（精确维度匹配） */
    private Long locationId;
    /** 批次号（精确维度匹配） */
    private String batchNo;
    /** 序列号（精确维度匹配） */
    private String serialNo;

    /** 物料/产品编码、名称、仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
    @TableField(exist = false)
    private String warehouseName;
}
