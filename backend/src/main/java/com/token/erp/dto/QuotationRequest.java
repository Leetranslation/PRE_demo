package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "报价单请求")
public class QuotationRequest {
    @Schema(description = "报价单ID（更新时）")
    private Long id;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "报价日期")
    private LocalDate quoteDate;
    @Schema(description = "有效期至")
    private LocalDate validUntil;
    @Schema(description = "业务员")
    private String salesman;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "报价明细")
    private List<Item> items;

    @Data
    @Schema(description = "报价单明细")
    public static class Item {
        @Schema(description = "产品ID")
        private Long productId;
        @Schema(description = "规格")
        private String spec;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Schema(description = "含税单价")
        private BigDecimal price;
        @Schema(description = "税率")
        private BigDecimal taxRate;
        @Schema(description = "备注")
        private String remark;
    }
}
