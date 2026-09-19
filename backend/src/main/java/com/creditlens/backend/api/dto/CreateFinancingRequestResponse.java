package com.creditlens.backend.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateFinancingRequestResponse(
        UUID id,
        UUID clientRequestId,
        ConsumerDto consumer,
        List<CreditRegisterExtractPurposeDto> creditRegisterExtractPurposes,
        Instant requestedAt,
        Instant completedAt,
        CreditExtractSummaryDto creditExtractSummary,
        @JsonIgnore boolean newlyCreated
) {
    public CreateFinancingRequestResponse(UUID id,
                                          UUID clientRequestId,
                                          ConsumerDto consumer,
                                          List<CreditRegisterExtractPurposeDto> creditRegisterExtractPurposes,
                                          Instant requestedAt,
                                          Instant completedAt,
                                          CreditExtractSummaryDto creditExtractSummary) {
        this(id, clientRequestId, consumer, creditRegisterExtractPurposes, requestedAt,
                completedAt, creditExtractSummary, false);
    }

    public CreateFinancingRequestResponse {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        creditRegisterExtractPurposes = List.copyOf(creditRegisterExtractPurposes);
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        Objects.requireNonNull(creditExtractSummary, "creditExtractSummary must not be null");
        if (completedAt.isBefore(requestedAt)) {
            throw new IllegalArgumentException("completedAt must not be before requestedAt");
        }
    }
}
