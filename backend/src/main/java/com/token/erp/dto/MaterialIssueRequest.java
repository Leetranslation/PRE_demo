package com.token.erp.dto;

import com.token.erp.entity.MaterialIssueItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialIssueRequest {
    private Long orderId;
    private Long warehouseId;
    private LocalDate issueDate;
    private String remark;
    private List<MaterialIssueItem> items;
}
