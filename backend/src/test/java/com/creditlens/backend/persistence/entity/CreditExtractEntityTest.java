package com.creditlens.backend.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditExtractData;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import com.creditlens.backend.domain.VoluntaryCreditBanReason;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreditExtractEntityTest {
  private static final Instant NOW = Instant.parse("2026-09-19T10:15:30Z");
  private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Test
  void mapsCreditExtractAndProtectsCollections() {
    CreditExtract extract = extract();
    FinancingRequestEntity request =
        new FinancingRequestEntity(
            FinancingRequest.create(
                REQUEST_ID,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                new Consumer(
                    UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    PersonalIdentityCode.of("010190-123A"),
                    NOW),
                List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
                NOW,
                NOW,
                extract()),
            consumerEntity());
    CreditExtractEntity entity = new CreditExtractEntity(extract, request);

    assertThat(entity.getId()).isEqualTo(extract.id());
    assertThat(entity.getFinancingRequestId()).isEqualTo(REQUEST_ID);
    assertThat(entity.getExtractReference()).isEqualTo(extract.extractReference());
    assertThat(entity.getCreationTimeUtc()).isEqualTo(NOW);
    assertThat(entity.isVoluntaryBanActive()).isTrue();
    assertThat(entity.getVoluntaryBanReason())
        .isEqualTo(VoluntaryCreditBanReason.RiskOfIdentityTheft.name());
    assertThat(entity.getLendersCount()).isEqualTo(2);
    assertThat(entity.getLoanContractsCount()).isEqualTo(3);
    assertThat(entity.getGuaranteedLoanContractsCount()).isEqualTo(1);
    assertThat(entity.getRepaymentAmounts())
        .containsExactlyElementsOf(extract.repaymentsPaidLastAmount());
    assertThat(entity.getLeasingInstalmentAmounts())
        .containsExactlyElementsOf(extract.sumOfMonthlyLeasingInstalments());
    assertThat(entity.getLoans()).containsExactlyElementsOf(extract.loans());
    assertThat(entity.getIncomeData()).containsExactlyElementsOf(extract.incomeData());
    assertThat(entity.getPersistedAt()).isEqualTo(NOW);
    assertThat(entity.toDomain()).isEqualTo(extract);
  }

  private ConsumerEntity consumerEntity() {
    return new ConsumerEntity(
        new Consumer(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            PersonalIdentityCode.of("010190-123A"),
            NOW));
  }

  private CreditExtract extract() {
    return new CreditExtract(
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        NOW,
        new VoluntaryBanOnCredits(true, VoluntaryCreditBanReason.RiskOfIdentityTheft),
        new CreditInformationSummary(2, 3, 1),
        List.of(new CreditExtractData.CurrencyAmount("EUR", null)),
        List.of(new CreditExtractData.CurrencyAmount("EUR", null)),
        List.of(),
        List.of(),
        NOW);
  }
}
