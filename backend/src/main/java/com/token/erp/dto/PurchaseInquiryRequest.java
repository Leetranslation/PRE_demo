package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "采购询价单请求")
public class PurchaseInquiryRequest {
    @Schema(description = "询价单ID（更新时）")
    private Long id;
    @Schema(description = "询价日期")
    private LocalDate inquiryDate;
    @Schema(description = "询价人")
    private String inquirer;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "询价明细")
    private List<Item> items;

    @Data
    @Schema(description = "询价明细")
    public static class Item {
        @Schema(description = "物料ID")
        private Long materialId;
        @Schema(description = "规格")
        private String spec;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "数量")
        private BigDecimal quantity;
        @Schema(description = "供应商ID")
        private Long supplierId;
        @Schema(description = "供应商报价单价")
        private BigDecimal price;
        @Schema(description = "备注")
        private String remark;
    }
}
