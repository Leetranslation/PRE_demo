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
    private BigDecimal invoicedAmount;
    private String remark;

    /** 客户名称（冗余展示字段） */
    @TableField(exist = false)
    private String customerName;
    /** 产品编号（冗余展示字段）：首个产品编号，多品种时加「等N种」 */
    @TableField(exist = false)
    private String productCode;
    /** 产品名称（冗余展示字段）：首个产品名称 */
    @TableField(exist = false)
    private String productName;
}
