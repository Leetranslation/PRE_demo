package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 交期风险评估结果的一行（对应前端「AI交期风险智能预警」弹窗里的表格行）。
 */
@Data
public class DeliveryRiskRow {

    /** 明细行 id */
    private Long itemId;
    private Long productId;
    private String productCode;
    private String productName;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private LocalDate deliveryDate;

    /** on_time / at_risk / late */
    private String riskLevel;
    /** 中文文案，前端直接显示 */
    private String riskText;

    /** AI 建议交付日期；可准时交付时为 null */
    private LocalDate suggestDate;
    /** 倒推出的最早必须行动日期 */
    private LocalDate earliestActionDate;

    private BigDecimal stockQty;
    private BigDecimal shortageQty;
    /** 本次用于倒推的周期天数 */
    private BigDecimal cycleDays;

    /** 计算过程（「查看分析」与 AI 报告用） */
    private Map<String, Object> detail;
    /** 缺料明细（用于报告第 3 段） */
    private List<Map<String, Object>> shortageItems;
}
