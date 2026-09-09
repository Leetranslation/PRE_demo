package com.token.erp.dto;

import com.token.erp.entity.MaterialReturnItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialReturnRequest {
    private String requestId;
    private Long orderId;
    private Long warehouseId;
    private LocalDate returnDate;
    private String remark;
    private List<MaterialReturnItem> items;
}
