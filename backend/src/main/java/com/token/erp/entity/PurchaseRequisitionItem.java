package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("purchase_requisition_item")
public class PurchaseRequisitionItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requisitionId;
    private Long materialId;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private LocalDate requiredDate;
    private String remark;
}
