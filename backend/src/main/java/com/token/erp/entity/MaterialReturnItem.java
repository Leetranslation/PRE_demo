package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("material_return_item")
public class MaterialReturnItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long returnId;
    private Long materialId;
    private Long locationId;
    private BigDecimal quantity;
    private String unit;

    /** 物料编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
