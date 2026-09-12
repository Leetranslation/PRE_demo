package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssemblyRequest {

    private String requestId;
    /** assembly组装 / disassembly拆卸 */
    private String type;
    private Long productId;
    private Long bomId;
    private BigDecimal quantity;
    private Long warehouseId;
    private LocalDate bizDate;
    private String remark;
}
