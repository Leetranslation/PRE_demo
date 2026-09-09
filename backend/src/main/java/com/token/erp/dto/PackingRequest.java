package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "装箱单请求")
public class PackingRequest {
    @Schema(description = "销售出库单ID")
    private Long outboundId;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "装箱明细")
    private List<Item> items;

    @Data
    @Schema(description = "装箱明细")
    public static class Item {
        @Schema(description = "箱号")
        private String boxNo;
        @Schema(description = "产品ID")
        private Long productId;
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Schema(description = "备注")
        private String remark;
    }
}
