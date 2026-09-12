package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 驾驶舱统计（9 KPI + 待办中心 + 图表）
 */
@Data
public class CockpitStats {

    // ===== 9 个 KPI =====
    private BigDecimal salesOrderAmount = BigDecimal.ZERO;     // 销售订单总金额
    private BigDecimal salesOnTimeRate = BigDecimal.ZERO;      // 销售订单准时率%
    private BigDecimal productionOnTimeRate = BigDecimal.ZERO; // 工单生产准时率%
    private BigDecimal productionOutput = BigDecimal.ZERO;     // 生产总产值
    private BigDecimal purchaseOrderAmount = BigDecimal.ZERO;  // 采购订单总金额
    private BigDecimal payableAmount = BigDecimal.ZERO;        // 应付金额
    private BigDecimal paidAmount = BigDecimal.ZERO;           // 已付金额
    private BigDecimal inventoryTurnover = BigDecimal.ZERO;    // 库存周转率
    private BigDecimal materialStockAmount = BigDecimal.ZERO;  // 物料库存金额
    private BigDecimal productStockAmount = BigDecimal.ZERO;   // 成品库存金额

    // ===== 待办中心 =====
    private List<TodoItem> todos = new ArrayList<>();

    // ===== 图表：销售趋势 =====
    private List<TrendItem> salesTrend = new ArrayList<>();

    // ===== 图表：库存分布 =====
    private List<StockItem> stockDistribution = new ArrayList<>();

    @Data
    public static class TodoItem {
        private String name;
        private long count;
        private String path;
    }

    @Data
    public static class TrendItem {
        private String month;
        private BigDecimal amount;
    }

    @Data
    public static class StockItem {
        private String name;
        private BigDecimal quantity;
    }
}
