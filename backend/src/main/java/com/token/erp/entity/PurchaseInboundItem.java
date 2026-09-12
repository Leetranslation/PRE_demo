package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("purchase_inbound_item")
public class PurchaseInboundItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundId;
    private Long orderItemId;
    private Long materialId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private BigDecimal quantity;
    private String unit;

    /** 物料编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
