package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("production_inbound_item")
public class ProductionInboundItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundId;
    private Long productId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private BigDecimal quantity;
    private String unit;

    /** 产品编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
}
