package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_return")
public class MaterialReturn extends BaseEntity {
    private String returnNo;
    private Long orderId;
    private Long warehouseId;
    private LocalDate returnDate;
    private String status;
    private String remark;
}
