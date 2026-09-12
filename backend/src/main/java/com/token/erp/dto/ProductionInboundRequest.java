package com.token.erp.dto;

import com.token.erp.entity.ProductionInboundItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProductionInboundRequest {
    private String requestId;
    /** production生产入库 / outsource_whole整单外协入库 / outsource_process工序外协入库 */
    private String inboundType;
    private Long orderId;
    private Long outsourceId;
    private Long warehouseId;
    private LocalDate inboundDate;
    private String remark;
    private List<ProductionInboundItem> items;
}
