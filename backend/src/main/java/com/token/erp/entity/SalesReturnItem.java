package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("sales_return_item")
public class SalesReturnItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long returnId;
    private Long productId;
    private BigDecimal quantity;
    private String unit;
    private String remark;

    /** 产品编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
}
