package com.token.erp.dto;

import com.token.erp.entity.ProductionInboundItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProductionInboundRequest {
    private String requestId;
    private Long orderId;
    private Long warehouseId;
    private LocalDate inboundDate;
    private String remark;
    private List<ProductionInboundItem> items;
}
