package com.token.erp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "采购申请单请求")
public class PurchaseRequisitionRequest {
    @Schema(description = "申请单ID（更新时）")
    private Long id;
    @Schema(description = "申请部门ID")
    private Long departmentId;
    @Schema(description = "申请日期")
    private LocalDate requisitionDate;
    @Schema(description = "申请人")
    private String applicant;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "申请明细")
    private List<Item> items;

    @Data
    @Schema(description = "采购申请明细")
    public static class Item {
        @Schema(description = "物料ID")
        private Long materialId;
        @Schema(description = "规格")
        private String spec;
        @Schema(description = "单位")
        private String unit;
        @Schema(description = "需求数量")
        private BigDecimal quantity;
        @Schema(description = "需求日期")
        private LocalDate requiredDate;
        @Schema(description = "备注")
        private String remark;
    }
}
