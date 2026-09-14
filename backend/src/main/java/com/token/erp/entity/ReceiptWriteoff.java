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
@TableName("receipt_writeoff")
public class ReceiptWriteoff implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiptId;
    private Long receivableId;
    private BigDecimal amount;
    private LocalDate writeoffDate;
    private String remark;
    private LocalDateTime createTime;

    /** 应收单号/客户名称（冗余展示字段） */
    @TableField(exist = false)
    private String receivableNo;
    @TableField(exist = false)
    private String customerName;
}
