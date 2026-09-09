package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("contract_item")
public class ContractItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private Long productId;
    private String spec;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal taxRate;
    private BigDecimal totalAmount;
    private String remark;
}
