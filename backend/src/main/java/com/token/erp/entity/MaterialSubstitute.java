package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_substitute")
public class MaterialSubstitute extends BaseEntity {

    private Long materialId;
    private Long substituteMaterialId;
    private BigDecimal ratio;
    private Integer priority;
    private Integer status;
    private String remark;
}
