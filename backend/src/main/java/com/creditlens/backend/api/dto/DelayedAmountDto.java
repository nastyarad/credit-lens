package com.creditlens.backend.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DelayedAmountDto(BigDecimal delayedInstalment, LocalDate originalDueDate, boolean isForeclosed) {
}
