package com.creditlens.backend.integration.pcr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creditlens.backend.domain.PersonalIdentityCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PcrCreditExtractMapperTest {
  private static final PersonalIdentityCode PERSON = PersonalIdentityCode.of("010190-123A");

  @Test
  void mapsFullExtractAndDefaultsOptionalCollections() {
    PcrTransport.Loan loan =
        new PcrTransport.Loan(
            "LumpSumLoan",
            "2026-01-01T00:00:00Z",
            true,
            List.of("ApartmentOrRealEstate"),
            1,
            "EUR",
            new PcrTransport.PaymentPlan(false, false),
            false,
            new PcrTransport.LumpSumLoan(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(9), "2030-01-01T00:00:00Z", 12),
            null,
            null,
            List.of(new PcrTransport.DelayedAmount(BigDecimal.ONE, "2026-02-01T00:00:00Z")),
            true);
    PcrTransport.CreditRegisterExtract source =
        new PcrTransport.CreditRegisterExtract(
            "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
            "2026-09-18T10:15:30Z",
            new PcrTransport.PersonRequested(PERSON.value()),
            null,
            new PcrTransport.Summary(2, 1, 0),
            null,
            List.of(new PcrTransport.Amount("EUR", BigDecimal.TEN)),
            List.of(loan),
            List.of(new PcrTransport.Income(2026, null)));

    var result = PcrCreditExtractMapper.map(source, PERSON);

    assertThat(result.extractReference().toString())
        .isEqualTo("2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10");
    assertThat(result.voluntaryBanOnCredits().isInEffect()).isFalse();
    assertThat(result.repaymentsPaidLastAmount()).isEmpty();
    assertThat(result.sumOfMonthlyLeasingInstalments()).hasSize(1);
    assertThat(result.loans()).hasSize(1);
    assertThat(result.loans().getFirst().delayedAmount().getFirst().isForeclosed()).isTrue();
    assertThat(result.incomeData().getFirst().months()).isEmpty();
  }

  @Test
  void mapsActiveBanReason() {
    PcrTransport.CreditRegisterExtract source =
        new PcrTransport.CreditRegisterExtract(
            "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
            "2026-09-18T10:15:30Z",
            new PcrTransport.PersonRequested(PERSON.value()),
            new PcrTransport.VoluntaryBan(true, "RiskOfIdentityTheft"),
            new PcrTransport.Summary(0, 0, 0),
            List.of(),
            List.of(),
            List.of(),
            List.of());

    assertThat(PcrCreditExtractMapper.map(source, PERSON).voluntaryBanOnCredits().reason())
        .hasToString("RiskOfIdentityTheft");
  }

  @Test
  void rejectsMissingAndInconsistentMandatoryData() {
    PcrTransport.CreditRegisterExtract missingPerson =
        new PcrTransport.CreditRegisterExtract(
            "not-a-uuid", "not-a-time", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> PcrCreditExtractMapper.map(missingPerson, PERSON))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void keepsDomainLoanEnumsStrict() {
    assertThatThrownBy(
            () ->
                PcrCreditExtractMapper.map(
                    new PcrTransport.CreditRegisterExtract(
                        "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
                        "2026-09-18T10:15:30Z",
                        new PcrTransport.PersonRequested(PERSON.value()),
                        null,
                        new PcrTransport.Summary(0, 0, 0),
                        List.of(),
                        List.of(),
                        List.of(
                            new PcrTransport.Loan(
                                "UnknownLoan",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)),
                        List.of()),
                    PERSON))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
