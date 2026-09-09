package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "主生产计划请求")
public class MpsPlanRequest {
    @Schema(description = "主生产计划ID（更新时）")
    private Long id;
    @Schema(description = "产品ID")
    private Long productId;
    @Schema(description = "计划数量")
    private BigDecimal quantity;
    @Schema(description = "计划生产日期")
    private LocalDate planDate;
    @Schema(description = "车间ID")
    private Long workshopId;
    @Schema(description = "备注")
    private String remark;
}
