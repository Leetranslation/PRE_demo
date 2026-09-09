package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("process_route_item")
public class ProcessRouteItem {
    private Long id;
    private Long routeId;
    private Long processId;
    private Integer seqNo;
    private BigDecimal standardHours;
    private Integer isKey;
    private Integer isQc;
    private String remark;
}
