package com.token.erp.dto;

import com.token.erp.entity.OutsourceOrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OutsourceOrderRequest {

    private Long id;
    /** whole整单外协 / process工序外协 */
    private String type;
    private Long orderId;
    private Long processId;
    private Long supplierId;
    private BigDecimal quantity;
    private LocalDate deliveryDate;
    private String remark;
    private List<OutsourceOrderItem> items;
}
