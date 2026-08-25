package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesReturnRequest {
    private Long orderId;
    private Long warehouseId;
    private Long productId;
    private BigDecimal quantity;
    private String remark;
}
