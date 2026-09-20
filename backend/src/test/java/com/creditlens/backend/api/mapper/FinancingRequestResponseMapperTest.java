package com.creditlens.backend.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
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

class FinancingRequestResponseMapperTest {
  @Test
  void mapsAndMasksCreateResponseForNewAndExistingRequests() {
    UUID requestId = UUID.randomUUID();
    UUID clientRequestId = UUID.randomUUID();
    UUID extractReference = UUID.randomUUID();
    Instant time = Instant.parse("2026-09-19T10:15:30Z");
    FinancingRequest source =
        FinancingRequest.create(
            requestId,
            clientRequestId,
            new Consumer(UUID.randomUUID(), PersonalIdentityCode.of("010190-123A"), time),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
            time,
            time,
            new CreditExtract(
                UUID.randomUUID(),
                extractReference,
                time,
                new VoluntaryBanOnCredits(true, VoluntaryCreditBanReason.RiskOfIdentityTheft),
                new CreditInformationSummary(2, 3, 1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                time));

    var created = FinancingRequestResponseMapper.toCreateResponse(source, true);
    var existing = FinancingRequestResponseMapper.toCreateResponse(source, false);

    assertThat(created.id()).isEqualTo(requestId);
    assertThat(created.clientRequestId()).isEqualTo(clientRequestId);
    assertThat(created.consumer().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(created.creditRegisterExtractPurposes())
        .containsExactly(CreditRegisterExtractPurposeDto.NewConsumerCredit);
    assertThat(created.creditExtractSummary().extractReference()).isEqualTo(extractReference);
    assertThat(created.creditExtractSummary().lendersCount()).isEqualTo(2);
    assertThat(created.creditExtractSummary().loanContractsCount()).isEqualTo(3);
    assertThat(created.creditExtractSummary().guaranteedLoanContractsCount()).isEqualTo(1);
    assertThat(created.creditExtractSummary().voluntaryBanOnCredits().reason())
        .isEqualTo(com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto.RiskOfIdentityTheft);
    assertThat(created.newlyCreated()).isTrue();
    assertThat(existing.newlyCreated()).isFalse();
  }
}
