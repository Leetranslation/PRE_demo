package com.token.erp.entity;

import com.token.erp.common.BaseEntity;
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
}
