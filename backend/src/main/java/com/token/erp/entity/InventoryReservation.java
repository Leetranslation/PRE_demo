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
@TableName("inventory_reservation")
public class InventoryReservation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inventoryId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private BigDecimal quantity;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime releaseTime;

    /** 物料/产品编码、名称（冗余展示字段，经 inventoryId 关联） */
    @TableField(exist = false)
    private String itemCode;
    @TableField(exist = false)
    private String itemName;
}
