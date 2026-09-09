package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("sales_stocking_item")
public class SalesStockingItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockingId;
    private Long productId;
    private BigDecimal quantity;
    private String unit;
    private String remark;
}
