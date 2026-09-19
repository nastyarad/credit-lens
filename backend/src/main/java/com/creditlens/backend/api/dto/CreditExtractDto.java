package com.creditlens.backend.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreditExtractDto(
        UUID extractReference,
        Instant creationTimeUtc,
        VoluntaryBanOnCreditsDto voluntaryBanOnCredits,
        CreditInformationSummaryDto creditInformationSummary,
        List<LoanDto> loans,
        List<IncomeDataDto> incomeData
) {

    public CreditExtractDto {
        Objects.requireNonNull(extractReference, "extractReference must not be null");
        Objects.requireNonNull(creationTimeUtc, "creationTimeUtc must not be null");
        Objects.requireNonNull(voluntaryBanOnCredits, "voluntaryBanOnCredits must not be null");
        Objects.requireNonNull(creditInformationSummary, "creditInformationSummary must not be null");
        loans = List.copyOf(loans);
        incomeData = List.copyOf(incomeData);
    }
}
