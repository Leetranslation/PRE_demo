package com.token.erp.dto;

import com.token.erp.entity.PurchaseInboundItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseInboundRequest {
    private Long orderId;
    private Long warehouseId;
    private LocalDate inboundDate;
    private String remark;
    private List<PurchaseInboundItem> items;
}
