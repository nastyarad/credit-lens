package com.creditlens.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeasingContractDto(LocalDate contractPeriodStartDate, BigDecimal transactionPrice) {}
