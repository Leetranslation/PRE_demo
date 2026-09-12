package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.token.erp.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_inbound")
public class ProductionInbound extends BaseEntity {
    private String inboundNo;
    private String inboundType;
    private Long orderId;
    private Long outsourceId;
    private Long warehouseId;
    private LocalDate inboundDate;
    private String status;
    private String remark;
}
