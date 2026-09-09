package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_adjustment")
public class StockAdjustment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String adjustNo;
    private Long itemId;
    private String itemType;
    private Long warehouseId;
    private Long locationId;
    private BigDecimal adjustQty;
    private String reason;
    private String sourceType;
    private Long sourceId;
    private String createBy;
    private LocalDateTime createTime;
}
