package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "采购退货请求")
public class PurchaseReturnRequest {
    @Schema(description = "采购订单ID")
    private Long orderId;
    @Schema(description = "出库仓库ID")
    private Long warehouseId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "退货明细")
    private List<Item> items;

    @Data
    @Schema(description = "退货明细")
    public static class Item {
        @Schema(description = "物料ID")
        private Long materialId;
        @Schema(description = "退货数量")
        private BigDecimal quantity;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "备注")
        private String remark;
    }
}
