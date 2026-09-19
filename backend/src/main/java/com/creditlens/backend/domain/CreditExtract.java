package com.creditlens.backend.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreditExtract(
        UUID id,
        UUID extractReference,
        Instant creationTimeUtc,
        VoluntaryBanOnCredits voluntaryBanOnCredits,
        CreditInformationSummary creditInformationSummary,
        List<CreditExtractData.CurrencyAmount> repaymentsPaidLastAmount,
        List<CreditExtractData.CurrencyAmount> sumOfMonthlyLeasingInstalments,
        List<CreditExtractData.Loan> loans,
        List<CreditExtractData.IncomeData> incomeData,
        Instant persistedAt
) {

    public CreditExtract {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(extractReference, "extractReference must not be null");
        Objects.requireNonNull(creationTimeUtc, "creationTimeUtc must not be null");
        Objects.requireNonNull(voluntaryBanOnCredits, "voluntaryBanOnCredits must not be null");
        Objects.requireNonNull(creditInformationSummary, "creditInformationSummary must not be null");
        repaymentsPaidLastAmount = List.copyOf(repaymentsPaidLastAmount);
        sumOfMonthlyLeasingInstalments = List.copyOf(sumOfMonthlyLeasingInstalments);
        loans = List.copyOf(loans);
        incomeData = List.copyOf(incomeData);
    }

    public CreditExtract withPersistenceMetadata(UUID id, Instant persistedAt) {
        return new CreditExtract(
                id,
                extractReference,
                creationTimeUtc,
                voluntaryBanOnCredits,
                creditInformationSummary,
                repaymentsPaidLastAmount,
                sumOfMonthlyLeasingInstalments,
                loans,
                incomeData,
                persistedAt
        );
    }
}
