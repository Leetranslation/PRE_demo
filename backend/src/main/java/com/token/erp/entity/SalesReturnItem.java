package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
}
