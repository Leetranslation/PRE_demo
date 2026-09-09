package com.token.erp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaterialShortageVO {

    private Long materialId;
    private String materialType;
    private String materialCode;
    private String materialName;
    private BigDecimal unitUsage;
    private BigDecimal lossRate;
    private BigDecimal requiredQty;
    private BigDecimal availableQty;
    private BigDecimal shortageQty;
}
