package com.token.erp.dto;

import com.token.erp.entity.SalesOrderItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SalesOrderRequest {
    private Long id;
    private Long customerId;
    private LocalDate orderDate;
    private String salesType;
    private String contact;
    private LocalDate deliveryDate;
    private String salesman;
    private Long departmentId;
    private String remark;
    private List<SalesOrderItem> items;
}
