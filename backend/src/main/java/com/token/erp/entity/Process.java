package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("process")
@Schema(description = "工序")
public class Process extends BaseEntity {
    @Schema(description = "工序编码（GX+日期+流水）")
    private String code;
    @Schema(description = "工序名称")
    private String name;
    @Schema(description = "工序类别（加工/检验/外协等）")
    private String category;
    @Schema(description = "标准单价（工价）")
    private BigDecimal standardPrice;
    @Schema(description = "计价方式：piece计件/time计时/fixed固定价")
    private String priceType;
    @Schema(description = "工序说明")
    private String description;
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
