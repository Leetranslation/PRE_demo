package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bom")
public class Bom extends BaseEntity {
    private String code;
    private Long productId;
    private String name;
    private Integer status;
    private String remark;

    /** 产品编码/名称（冗余展示字段） */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
}
