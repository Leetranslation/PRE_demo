package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("stocktake_item")
public class StocktakeItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stocktakeId;
    private Long itemId;
    private String itemType;
    private Long locationId;
    private String batchNo;
    private BigDecimal bookQty;
    private BigDecimal actualQty;
    private BigDecimal diffQty;

    /** 物料/产品编码、名称（冗余展示字段） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
}
