package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_order")
public class SalesOrder extends BaseEntity {
    private String orderNo;
    private Long customerId;
    private LocalDate orderDate;
    private String salesType;
    private String contact;
    private LocalDate deliveryDate;
    private String salesman;
    private Long departmentId;
    private String approveStatus;
    private String bizStatus;
    private BigDecimal totalAmount;
    private String remark;
}
