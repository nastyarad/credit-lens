package com.creditlens.backend.api.dto;

import java.math.BigDecimal;

public record MonthlyIncomeDto(
        int month,
        BigDecimal wagesGrossAmount,
        BigDecimal wagesNetAmount,
        BigDecimal benefitsGrossAmount,
        BigDecimal benefitsNetAmount
) {

    public MonthlyIncomeDto {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
    }
}
