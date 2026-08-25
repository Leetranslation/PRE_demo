package com.token.erp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MrpRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
