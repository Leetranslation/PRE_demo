package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "库存预警")
public class InventoryWarningVO {
    @Schema(description = "物料/产品ID")
    private Long itemId;
    @Schema(description = "类型：material物料/product产品")
    private String itemType;
    @Schema(description = "编码")
    private String code;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "当前可用库存")
    private BigDecimal availableQty;
    @Schema(description = "最低库存")
    private BigDecimal minStock;
    @Schema(description = "安全库存")
    private BigDecimal safetyStock;
    @Schema(description = "预警级别：critical低于最低库存/warning低于安全库存")
    private String warningLevel;
}
