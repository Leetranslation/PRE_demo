package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 交期风险评估结果快照。
 *
 * 为什么要落表：列表页要按「风险等级」筛选、驾驶舱要统计，实时算做不到；
 * 但库存/单据随时在变，所以打开评估弹窗时会**实时重算并覆盖**这条记录，
 * 表里的值只当缓存用。
 */
@Data
@TableName("delivery_risk")
public class DeliveryRisk implements Serializable {

    public static final String BIZ_SALES_ORDER = "sales_order";
    public static final String BIZ_QUOTATION = "quotation";
    public static final String BIZ_CONTRACT = "contract";

    public static final String LEVEL_ON_TIME = "on_time";
    public static final String LEVEL_AT_RISK = "at_risk";
    public static final String LEVEL_LATE = "late";

    @TableId(type = IdType.AUTO)
    private Long id;
    /** sales_order / quotation / contract */
    private String bizType;
    private Long bizId;
    /** 明细行 id */
    private Long itemId;
    /** product / material */
    private String itemType;
    private BigDecimal qty;
    private LocalDate deliveryDate;
    /** on_time / at_risk / late */
    private String riskLevel;
    private LocalDate earliestActionDate;
    private LocalDate suggestDate;
    private BigDecimal stockQty;
    private BigDecimal shortageQty;
    /** 本次用于倒推的周期天数 */
    private BigDecimal cycleDays;
    /** 完整计算过程 JSON（给「查看分析」和 AI 报告用） */
    private String detailJson;
    private LocalDateTime calcTime;
}
