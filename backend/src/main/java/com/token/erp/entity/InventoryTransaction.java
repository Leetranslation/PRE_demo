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
@TableName("inventory_transaction")
public class InventoryTransaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String itemType;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private String stockType;
    private String direction;
    private BigDecimal quantity;
    private BigDecimal balanceQty;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String createBy;
    private LocalDateTime createTime;

    /** 物料/产品编码、名称、仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
    @TableField(exist = false)
    private String warehouseName;
}
