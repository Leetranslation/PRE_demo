package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assembly_order")
public class AssemblyOrder extends BaseEntity {

    private String orderNo;
    private String type;
    private Long productId;
    private Long bomId;
    private BigDecimal quantity;
    private Long warehouseId;
    private LocalDate bizDate;
    private String status;
    private String remark;

    /** 产品编码/名称、仓库名称（冗余展示字段） */
    @TableField(exist = false)
    private String productCode;
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String warehouseName;
}
