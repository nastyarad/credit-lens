package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FinancingRequestHistoryItemDto(
    UUID id,
    UUID clientRequestId,
    String maskedPersonalIdentityCode,
    Instant requestedAt,
    Instant completedAt,
    UUID extractReference,
    Boolean voluntaryCreditBanActive) {

  public FinancingRequestHistoryItemDto {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
    Objects.requireNonNull(
        maskedPersonalIdentityCode, "maskedPersonalIdentityCode must not be null");
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    Objects.requireNonNull(completedAt, "completedAt must not be null");
    Objects.requireNonNull(extractReference, "extractReference must not be null");
    Objects.requireNonNull(voluntaryCreditBanActive, "voluntaryCreditBanActive must not be null");
  }
}
