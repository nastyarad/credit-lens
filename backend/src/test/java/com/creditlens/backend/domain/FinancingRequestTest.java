package com.creditlens.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancingRequestTest {
  private static final Instant REQUESTED_AT = Instant.parse("2026-09-19T10:15:29Z");
  private static final Instant COMPLETED_AT = Instant.parse("2026-09-19T10:15:30Z");

  @Test
  void createsAnImmutableCompletedRequestWithAnExtract() {
    CreditExtract extract = extract();
    FinancingRequest request = request(COMPLETED_AT, extract);

    assertThat(request.completedAt()).isEqualTo(COMPLETED_AT);
    assertThat(request.creditExtract()).isEqualTo(extract);
    assertThat(request.extractPurposes())
        .containsExactly(CreditRegisterExtractPurpose.NewConsumerCredit);
  }

  @Test
  void rejectsCompletionBeforeRequest() {
    assertThatThrownBy(() -> request(REQUESTED_AT.minusSeconds(1), extract()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("completion time must not be before request time");
  }

  @Test
  void rejectsRequestWithoutCreditExtract() {
    assertThatThrownBy(() -> request(COMPLETED_AT, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("creditExtract must not be null");
  }

  @Test
  void rejectsRequestWithoutPurposes() {
    assertThatThrownBy(
            () ->
                FinancingRequest.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    consumer(),
                    List.of(),
                    REQUESTED_AT,
                    COMPLETED_AT,
                    extract()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("at least one extract purpose is required");
  }

  @Test
  void keepsExtractAndPurposesImmutable() {
    CreditExtract extract = extract();
    FinancingRequest request = request(COMPLETED_AT, extract);

    assertThat(request.creditExtract()).isSameAs(extract);
    assertThatThrownBy(() -> request.extractPurposes().add(CreditRegisterExtractPurpose.NewLoan))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private FinancingRequest request(Instant completedAt, CreditExtract extract) {
    return FinancingRequest.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        consumer(),
        List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
        REQUESTED_AT,
        completedAt,
        extract);
  }

  private Consumer consumer() {
    return new Consumer(UUID.randomUUID(), PersonalIdentityCode.of("010190-123A"), REQUESTED_AT);
  }

  private CreditExtract extract() {
    return new CreditExtract(
        UUID.randomUUID(),
        UUID.randomUUID(),
        COMPLETED_AT,
        new VoluntaryBanOnCredits(false, null),
        new CreditInformationSummary(0, 0, 0),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        COMPLETED_AT);
  }
}
