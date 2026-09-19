package com.creditlens.backend.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancingRequestEntityTest {
  private static final Instant NOW = Instant.parse("2026-09-19T10:15:30Z");
  private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID CLIENT_REQUEST_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void mapsFinancingRequestAndCopiesPurposes() {
    Consumer consumer =
        new Consumer(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            PersonalIdentityCode.of("010190-123A"),
            NOW);
    ConsumerEntity consumerEntity = new ConsumerEntity(consumer);
    FinancingRequest request =
        FinancingRequest.create(
            REQUEST_ID,
            CLIENT_REQUEST_ID,
            consumer,
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
            NOW,
            NOW,
            extract());

    FinancingRequestEntity entity = new FinancingRequestEntity(request, consumerEntity);
    String[] purposes = entity.getExtractPurposes();
    purposes[0] = "changed";

    assertThat(entity.getId()).isEqualTo(REQUEST_ID);
    assertThat(entity.getConsumer()).isSameAs(consumerEntity);
    assertThat(entity.getClientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
    assertThat(entity.getExtractPurposes())
        .containsExactly(CreditRegisterExtractPurpose.NewConsumerCredit.name());
    assertThat(entity.getRequestedAt()).isEqualTo(NOW);
    assertThat(entity.getCompletedAt()).isEqualTo(NOW);

    FinancingRequest restored = entity.toDomain(extract());
    assertThat(restored.id()).isEqualTo(request.id());
    assertThat(restored.clientRequestId()).isEqualTo(request.clientRequestId());
    assertThat(restored.consumer()).isEqualTo(request.consumer());
    assertThat(restored.extractPurposes()).containsExactlyElementsOf(request.extractPurposes());
    assertThat(restored.requestedAt()).isEqualTo(request.requestedAt());
    assertThat(restored.completedAt()).isEqualTo(request.completedAt());
    assertThat(restored.creditExtract()).isEqualTo(request.creditExtract());
  }

  private CreditExtract extract() {
    return new CreditExtract(
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        NOW,
        new VoluntaryBanOnCredits(false, null),
        new CreditInformationSummary(0, 0, 0),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        NOW);
  }
}
