package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物料/产品的历史周期均值（预计算）。
 *
 * 交期评估每次都要「按历史单据算平均周期」，实时算要扫一年的单据，太慢，
 * 所以单独存一张表，由 {@code recalcCycleStats()} 定期/手动刷新。
 */
@Data
@TableName("item_cycle_stat")
public class ItemCycleStat implements Serializable {

    public static final String TYPE_PRODUCTION = "production";
    public static final String TYPE_PURCHASE = "purchase";
    public static final String TYPE_OUTSOURCE = "outsource";

    @TableId(type = IdType.AUTO)
    private Long id;
    /** product / material */
    private String itemType;
    private Long itemId;
    /** production / purchase / outsource */
    private String cycleType;
    /** 平均周期：生产为「天/件」，采购/外协为「整批天」 */
    private BigDecimal avgDays;
    private Integer sampleCount;
    private Integer windowDays;
    private LocalDateTime calcTime;
}
