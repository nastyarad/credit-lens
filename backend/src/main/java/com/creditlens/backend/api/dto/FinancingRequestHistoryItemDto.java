package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FinancingRequestHistoryItemDto(
        UUID id,
        UUID clientRequestId,
        String maskedPersonalIdentityCode,
        FinancingRequestStatusDto status,
        Instant requestedAt,
        Instant completedAt,
        UUID extractReference,
        Boolean voluntaryCreditBanActive
) {

    public FinancingRequestHistoryItemDto {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
        Objects.requireNonNull(maskedPersonalIdentityCode, "maskedPersonalIdentityCode must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
