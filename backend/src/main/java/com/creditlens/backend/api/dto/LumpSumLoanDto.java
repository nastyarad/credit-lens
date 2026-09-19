package com.creditlens.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LumpSumLoanDto(
        BigDecimal amountIssued,
        BigDecimal amountPaid,
        BigDecimal balance,
        LocalDate plannedFinalDueDate,
        Integer amortizationFrequency
) {
}
