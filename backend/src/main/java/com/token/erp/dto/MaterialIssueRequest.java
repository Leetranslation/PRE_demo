package com.token.erp.dto;

import com.token.erp.entity.MaterialIssueItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialIssueRequest {
    private String requestId;
    /** production生产领料 / outsource_whole整单外协领料 / outsource_process工序外协领料 */
    private String issueType;
    private Long orderId;
    private Long outsourceId;
    private Long warehouseId;
    private LocalDate issueDate;
    private String remark;
    private List<MaterialIssueItem> items;
}
