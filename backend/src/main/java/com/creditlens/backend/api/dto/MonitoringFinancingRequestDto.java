package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MonitoringFinancingRequestDto(
        UUID financingRequestId,
        UUID extractReference,
        Instant requestedAt,
        Instant completedAt,
        String maskedPersonalIdentityCode,
        VoluntaryCreditBanReasonDto voluntaryCreditBanReason,
        int lendersCount,
        int loanContractsCount,
        int guaranteedLoanContractsCount
) {

    public MonitoringFinancingRequestDto {
        Objects.requireNonNull(financingRequestId, "financingRequestId must not be null");
        Objects.requireNonNull(extractReference, "extractReference must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        Objects.requireNonNull(maskedPersonalIdentityCode, "maskedPersonalIdentityCode must not be null");
        Objects.requireNonNull(voluntaryCreditBanReason, "voluntaryCreditBanReason must not be null");
        if (lendersCount < 0 || loanContractsCount < 0 || guaranteedLoanContractsCount < 0) {
            throw new IllegalArgumentException("credit information counts must not be negative");
        }
    }
}
