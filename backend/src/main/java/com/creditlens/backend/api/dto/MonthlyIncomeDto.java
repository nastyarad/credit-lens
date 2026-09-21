package com.creditlens.backend.api.dto;

import java.math.BigDecimal;

public record MonthlyIncomeDto(
    int month,
    BigDecimal wagesGrossAmount,
    BigDecimal wagesNetAmount,
    BigDecimal benefitsGrossAmount,
    BigDecimal benefitsNetAmount) {}
