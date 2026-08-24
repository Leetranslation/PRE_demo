package com.token.erp.entity;

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
}
