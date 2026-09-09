package com.token.erp.dto;

import com.token.erp.entity.SalesOutboundItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOutboundRequest {
    private String requestId;
    private Long orderId;
    private Long warehouseId;
    private LocalDate outboundDate;
    private String remark;
    private List<SalesOutboundItem> items;
}
