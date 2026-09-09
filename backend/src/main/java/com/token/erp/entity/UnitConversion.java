package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("unit_conversion")
@Schema(description = "多单位转换率")
public class UnitConversion extends BaseEntity {
    @Schema(description = "源单位ID")
    private Long unitId;
    @Schema(description = "目标单位ID")
    private Long targetUnitId;
    @Schema(description = "换算率（1源单位 = rate目标单位）")
    private BigDecimal rate;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
