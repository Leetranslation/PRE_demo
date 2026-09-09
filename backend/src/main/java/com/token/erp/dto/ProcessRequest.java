package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工序相关请求 DTO（创建/更新/批量设置）
 */
@Data
@Schema(description = "工序请求参数")
public class ProcessRequest {
    @Schema(description = "工序ID（更新时必填）")
    private Long id;
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

    @Schema(description = "批量操作的工序ID集合")
    private List<Long> ids;
    @Schema(description = "批量设置的标准单价")
    private BigDecimal price;
    @Schema(description = "批量设置的计价方式")
    private String priceTypeValue;
}
