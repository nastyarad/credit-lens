package com.creditlens.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RunningAccountLoanDto(
    BigDecimal creditLimit, BigDecimal balance, LocalDate balanceDate) {}
