package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sales_outbound")
public class SalesOutbound extends BaseEntity {
    private String outboundNo;
    private Long orderId;
    private Long warehouseId;
    private LocalDate outboundDate;
    private String status;
    private String remark;
}
