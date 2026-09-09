package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "销售备货单请求")
public class SalesStockingRequest {
    @Schema(description = "销售订单ID（可选）")
    private Long orderId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "备货明细")
    private List<Item> items;

    @Data
    @Schema(description = "备货明细")
    public static class Item {
        @Schema(description = "产品ID")
        private Long productId;
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "备注")
        private String remark;
    }
}
