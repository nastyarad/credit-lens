package com.creditlens.backend.api.dto;

public record PaymentPlanDto(
    boolean isInDebtArrangement, boolean isInBusinessRestructuringProgram) {}
