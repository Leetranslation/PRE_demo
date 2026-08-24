package com.token.erp.dto;

import com.token.erp.entity.PurchaseOrderItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单创建/更新请求（主表+明细）
 */
@Data
public class PurchaseOrderRequest {
    private Long id;
    private Long supplierId;
    private LocalDate orderDate;
    private String purchaseType;
    private LocalDate expectedDate;
    private String contact;
    private Long departmentId;
    private String buyer;
    private String remark;
    private List<PurchaseOrderItem> items;
}
