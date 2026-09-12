package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String itemType;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private String stockType;
    private BigDecimal quantity;
    private BigDecimal reservedQty;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 物料/产品编码、名称、仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
    @TableField(exist = false)
    private String warehouseName;
}
