package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payment_writeoff")
public class PaymentWriteoff implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paymentId;
    private Long payableId;
    private BigDecimal amount;
    private LocalDate writeoffDate;
    private String remark;
    private LocalDateTime createTime;

    /** 应付单号/供应商名称（冗余展示字段） */
    @TableField(exist = false)
    private String payableNo;
    @TableField(exist = false)
    private String supplierName;
}
