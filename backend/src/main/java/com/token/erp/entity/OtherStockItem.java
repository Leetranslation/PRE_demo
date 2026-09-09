package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("other_stock_item")
public class OtherStockItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockId;
    private Long itemId;
    private String itemType;
    private BigDecimal quantity;
    private String unit;
    private String remark;
}
