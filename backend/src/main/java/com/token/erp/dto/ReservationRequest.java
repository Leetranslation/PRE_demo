package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "锁库请求")
public class ReservationRequest {
    @Schema(description = "物料/产品ID")
    private Long itemId;
    @Schema(description = "类型：material物料/product产品")
    private String itemType;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "库位ID")
    private Long locationId;
    @Schema(description = "批号")
    private String batchNo;
    @Schema(description = "序列号")
    private String serialNo;
    @Schema(description = "库存类型：available可用/pending_qc待检")
    private String stockType;
    @Schema(description = "锁定数量")
    private BigDecimal quantity;
    @Schema(description = "业务类型（如 sales_outbound）")
    private String bizType;
    @Schema(description = "业务单据ID")
    private Long bizId;
    @Schema(description = "业务单据编号")
    private String bizNo;
}
