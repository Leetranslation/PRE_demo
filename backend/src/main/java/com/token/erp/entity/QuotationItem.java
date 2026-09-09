package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("quotation_item")
public class QuotationItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long quotationId;
    private Long productId;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal amountExTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remark;
}
