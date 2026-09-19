package com.creditlens.backend.api.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DtoSerializationTest {
    @Autowired
    JsonMapper jsonMapper;

    @Test
    void createResponseDoesNotExposeStatusErrorOrInternalCreationFlag() throws Exception {
        CreateFinancingRequestResponse response = new CreateFinancingRequestResponse(
                UUID.randomUUID(), UUID.randomUUID(), new ConsumerDto(UUID.randomUUID(), "******-123A"),
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
                Instant.parse("2026-09-18T10:15:29Z"), Instant.parse("2026-09-18T10:15:30Z"),
                new CreditExtractSummaryDto(UUID.randomUUID(), Instant.parse("2026-09-18T10:15:30Z"),
                        new VoluntaryBanOnCreditsDto(false, null), 0, 0, 0), true);
        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(response));
        assertThat(json.has("status")).isFalse();
        assertThat(json.has("error")).isFalse();
        assertThat(json.has("newlyCreated")).isFalse();
        assertThat(json.at("/consumer/maskedPersonalIdentityCode").asText()).isEqualTo("******-123A");
    }

    @Test
    void deserializesTheCreateRequest() throws Exception {
        CreateFinancingRequestRequest request = jsonMapper.readValue("""
                {"clientRequestId":"ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
                 "personalIdentityCode":"010190-123A",
                 "creditRegisterExtractPurposes":["NewConsumerCredit"]}
                """, CreateFinancingRequestRequest.class);
        assertThat(request.creditRegisterExtractPurposes()).containsExactly(CreditRegisterExtractPurposeDto.NewConsumerCredit);
    }

    @Test
    void historyAndDetailsContainOnlySuccessfulFetches() {
        FinancingRequestHistoryItemDto history = new FinancingRequestHistoryItemDto(
                UUID.randomUUID(), UUID.randomUUID(), "******-123A", Instant.EPOCH, Instant.EPOCH,
                UUID.randomUUID(), false);
        FinancingRequestDetailsDto details = new FinancingRequestDetailsDto(
                history.id(), history.clientRequestId(), new ConsumerDto(UUID.randomUUID(), "******-123A"),
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit), Instant.EPOCH, Instant.EPOCH,
                new CreditExtractDto(UUID.randomUUID(), Instant.EPOCH,
                        new VoluntaryBanOnCreditsDto(false, null),
                        new CreditInformationSummaryDto(0, 0, 0, List.of(), List.of()), List.of(), List.of()));
        assertThat(history.completedAt()).isEqualTo(Instant.EPOCH);
        assertThat(details.creditExtract()).isNotNull();
    }
}
