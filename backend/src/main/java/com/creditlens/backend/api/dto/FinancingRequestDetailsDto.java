package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record FinancingRequestDetailsDto(
    UUID id,
    UUID clientRequestId,
    ConsumerDto consumer,
    List<CreditRegisterExtractPurposeDto> creditRegisterExtractPurposes,
    Instant requestedAt,
    Instant completedAt,
    CreditExtractDto creditExtract) {

  public FinancingRequestDetailsDto {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(clientRequestId, "clientRequestId must not be null");
    Objects.requireNonNull(consumer, "consumer must not be null");
    creditRegisterExtractPurposes = List.copyOf(creditRegisterExtractPurposes);
    Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    Objects.requireNonNull(completedAt, "completedAt must not be null");
    Objects.requireNonNull(creditExtract, "creditExtract must not be null");
    if (completedAt.isBefore(requestedAt)) {
      throw new IllegalArgumentException("completedAt must not be before requestedAt");
    }
  }
}
