package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "工序进度")
public class ProcessProgressVO {
    @Schema(description = "工序ID")
    private Long processId;
    @Schema(description = "工序名称")
    private String processName;
    @Schema(description = "顺序号")
    private Integer seqNo;
    @Schema(description = "计划数量（工单数量）")
    private BigDecimal planQty;
    @Schema(description = "累计报工良品数")
    private BigDecimal reportedQty;
    @Schema(description = "报工次数")
    private Long reportCount;
    @Schema(description = "是否关键工序")
    private Integer isKey;
    @Schema(description = "是否检验工序")
    private Integer isQc;
}
