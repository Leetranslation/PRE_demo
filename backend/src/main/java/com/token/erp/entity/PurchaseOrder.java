package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("purchase_order")
public class PurchaseOrder extends BaseEntity {
    private String orderNo;
    private Long supplierId;
    private LocalDate orderDate;
    private String purchaseType;
    private LocalDate expectedDate;
    private String contact;
    private Long departmentId;
    private String buyer;
    private String sourceType;
    private Long sourceId;
    private String approveStatus;
    private String bizStatus;
    private BigDecimal totalAmount;
    private BigDecimal invoicedAmount;
    private String remark;

    /** 供应商名称（冗余展示字段） */
    @TableField(exist = false)
    private String supplierName;
    /** 物料编号（冗余展示字段）：首个物料编号，多种类时加「等N种」 */
    @TableField(exist = false)
    private String materialCode;
    /** 物料名称（冗余展示字段）：首个物料名称 */
    @TableField(exist = false)
    private String materialName;
}
