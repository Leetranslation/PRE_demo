package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("sales_order_item")
public class SalesOrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amountExTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDate deliveryDate;
    private BigDecimal outboundQty;
    private String remark;
}
