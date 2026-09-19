package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CreditExtractSummaryDto(
    UUID extractReference,
    Instant creationTimeUtc,
    VoluntaryBanOnCreditsDto voluntaryBanOnCredits,
    int lendersCount,
    int loanContractsCount,
    int guaranteedLoanContractsCount) {

  public CreditExtractSummaryDto {
    Objects.requireNonNull(extractReference, "extractReference must not be null");
    Objects.requireNonNull(creationTimeUtc, "creationTimeUtc must not be null");
    Objects.requireNonNull(voluntaryBanOnCredits, "voluntaryBanOnCredits must not be null");
    if (lendersCount < 0 || loanContractsCount < 0 || guaranteedLoanContractsCount < 0) {
      throw new IllegalArgumentException("credit information counts must not be negative");
    }
  }
}
