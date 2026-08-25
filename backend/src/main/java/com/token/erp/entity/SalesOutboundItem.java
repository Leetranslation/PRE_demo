package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("sales_outbound_item")
public class SalesOutboundItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundId;
    private Long orderItemId;
    private Long productId;
    private Long locationId;
    private String batchNo;
    private String serialNo;
    private BigDecimal quantity;
    private String unit;
}
