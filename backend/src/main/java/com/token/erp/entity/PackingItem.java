package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("packing_item")
public class PackingItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long packingId;
    private String boxNo;
    private Long productId;
    private BigDecimal quantity;
    private String remark;

    /** 非表字段：明细展示用，避免前端只看到产品 ID */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
}
