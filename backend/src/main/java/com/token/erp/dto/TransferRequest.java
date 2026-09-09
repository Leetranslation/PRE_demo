package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class TransferRequest {

    private Long sourceWarehouseId;
    private Long targetWarehouseId;
    private LocalDate transferDate;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long itemId;
        private String itemType;
        private BigDecimal quantity;
        private String unit;
    }
}
