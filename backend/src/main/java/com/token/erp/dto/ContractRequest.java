package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "合同请求")
public class ContractRequest {
    @Schema(description = "合同ID（更新时）")
    private Long id;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "签订日期")
    private LocalDate contractDate;
    @Schema(description = "业务员")
    private String salesman;
    @Schema(description = "付款方式/条款")
    private String paymentTerms;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "合同明细")
    private List<Item> items;

    @Data
    @Schema(description = "合同明细")
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
