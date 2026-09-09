package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("purchase_inquiry_item")
public class PurchaseInquiryItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inquiryId;
    private Long materialId;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private Long supplierId;
    private BigDecimal price;
    private String remark;
}
