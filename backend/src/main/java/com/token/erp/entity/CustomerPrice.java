package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_price")
public class CustomerPrice extends BaseEntity {

    private Long customerId;
    private Long itemId;
    private String itemType;
    private BigDecimal price;
    private Integer status;
    private String remark;
}
