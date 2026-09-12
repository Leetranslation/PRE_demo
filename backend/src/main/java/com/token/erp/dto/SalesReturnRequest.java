package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "销售退货请求")
public class SalesReturnRequest {
    @Schema(description = "幂等键（前端生成 UUID）")
    private String requestId;
    @Schema(description = "销售订单ID")
    private Long orderId;
    @Schema(description = "入库仓库ID")
    private Long warehouseId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "退货明细")
    private List<Item> items;

    @Data
    @Schema(description = "退货明细")
    public static class Item {
        @Schema(description = "产品ID")
        private Long productId;
        @Schema(description = "退货数量")
        private BigDecimal quantity;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "备注")
        private String remark;
    }
}
