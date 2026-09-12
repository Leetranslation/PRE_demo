package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("bom_item")
public class BomItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private Long bomVersionId;
    private Long childId;
    private String childType;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal lossRate;
    private String process;
    private Integer sort;
    private String remark;

    /** 子项编码/名称（冗余展示字段，按 childType 关联产品或物料） */
    @TableField(exist = false)
    private String childCode;
    @TableField(exist = false)
    private String childName;
}
