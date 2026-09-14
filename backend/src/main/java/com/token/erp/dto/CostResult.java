package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CostResult {
    private Long orderId;
    private String orderNo;
    private BigDecimal completedQty;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal overheadCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;
}
