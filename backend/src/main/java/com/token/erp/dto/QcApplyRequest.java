package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "质检申请单请求")
public class QcApplyRequest {
    @Schema(description = "质检来源（四线）：purchase_qc/production_qc/process_qc/outbound_qc")
    private String sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceId;
    @Schema(description = "来源单据号")
    private String sourceNo;
    @Schema(description = "物料/产品ID")
    private Long itemId;
    @Schema(description = "类型：material/product")
    private String itemType;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "申请质检数量")
    private BigDecimal quantity;
    @Schema(description = "申请日期")
    private LocalDate applyDate;
    @Schema(description = "备注")
    private String remark;
}
