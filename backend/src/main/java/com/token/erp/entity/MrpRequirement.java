package com.token.erp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mrp_requirement")
public class MrpRequirement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long itemId;
    private String itemType;
    private BigDecimal grossQty;
    private BigDecimal availableQty;
    private BigDecimal inTransitQty;
    private BigDecimal netQty;
    private String supplyType;
    private LocalDate demandDate;
    private LocalDate suggestDate;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private BigDecimal convertedQty;
    private String convertedStatus;
    private LocalDateTime createTime;
}
