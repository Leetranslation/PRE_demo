package com.token.erp.dto;

import lombok.Data;

import java.util.List;

@Data
public class MrpConfirmRequest {
    private Long planId;
    /** 要转换的建议ID列表 */
    private List<Long> requirementIds;
}
