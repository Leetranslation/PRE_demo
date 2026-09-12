package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 驾驶舱统计
 */
@Data
public class DashboardStats {
    // 待办数量
    private long pendingSalesApprove = 0;      // 待审批销售单
    private long pendingPurchaseApprove = 0;   // 待审批采购单
    private long pendingProductionApprove = 0; // 待审批生产工单
    private long pendingInbound = 0;           // 待入库采购单
    private long pendingOutbound = 0;          // 待出库销售单

    // 核心指标
    private long customerCount = 0;
    private long supplierCount = 0;
    private long materialCount = 0;
    private long productCount = 0;

    // 低库存预警
    private List<LowStockItem> lowStockItems = new ArrayList<>();

    @Data
    public static class LowStockItem {
        private Long itemId;
        private String itemType;
        private BigDecimal quantity;
        private BigDecimal safetyStock;
        private String itemCode;
        private String itemName;
    }
}
