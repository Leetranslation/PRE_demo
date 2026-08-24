package com.token.erp.entity;

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
    private String remark;
}
