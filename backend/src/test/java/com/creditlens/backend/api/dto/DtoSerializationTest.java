package com.creditlens.backend.api.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoSerializationTest {

    private final JsonMapper jsonMapper = JsonMapper.shared();

    @Test
    void serializesDetailsUsingThePublicApiContract() throws Exception {
        UUID requestId = UUID.fromString("d46c2b84-d979-43e2-b0e4-bc7de12d2454");
        UUID clientRequestId = UUID.fromString("ec2364ec-b4ed-4af7-805c-2bd44c42b9d5");
        UUID consumerId = UUID.fromString("d2fbb47f-2317-4740-8fa5-50f73b64182d");
        UUID extractReference = UUID.fromString("2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10");
        Instant completedAt = Instant.parse("2026-09-18T10:15:30Z");

        FinancingRequestDetailsDto dto = new FinancingRequestDetailsDto(
                requestId,
                clientRequestId,
                new ConsumerDto(consumerId, "******-123A"),
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
                FinancingRequestStatusDto.COMPLETED,
                Instant.parse("2026-09-18T10:15:29Z"),
                completedAt,
                null,
                new CreditExtractDto(
                        extractReference,
                        completedAt,
                        new VoluntaryBanOnCreditsDto(
                                true,
                                VoluntaryCreditBanReasonDto.ControlOfPersonalFinances
                        ),
                        new CreditInformationSummaryDto(
                                2,
                                3,
                                0,
                                List.of(new CurrencyAmountDto("EUR", new BigDecimal("450.00"))),
                                List.of()
                        ),
                        List.of(new LoanDto(
                                LoanTypeDto.LumpSumLoan,
                                LocalDate.parse("2024-01-15"),
                                false,
                                List.of(),
                                1,
                                "EUR",
                                new PaymentPlanDto(false, false),
                                false,
                                new LumpSumLoanDto(
                                        new BigDecimal("5000.00"),
                                        new BigDecimal("1800.00"),
                                        new BigDecimal("3200.00"),
                                        LocalDate.parse("2028-01-15"),
                                        1
                                ),
                                null,
                                null,
                                List.of()
                        )),
                        List.of()
                )
        );

        JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(dto));

        assertThat(json.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(json.get("creditRegisterExtractPurposes").get(0).asString())
                .isEqualTo("NewConsumerCredit");
        assertThat(json.get("creditExtract").get("creationTimeUtc").asString())
                .isEqualTo("2026-09-18T10:15:30Z");
        assertThat(json.get("creditExtract").get("voluntaryBanOnCredits").get("isInEffect").asBoolean())
                .isTrue();
        assertThat(json.get("creditExtract").get("loans").get(0).get("contractDate").asString())
                .isEqualTo("2024-01-15");
        assertThat(json.get("creditExtract").get("loans").get(0).get("runningAccountLoan").isNull())
                .isTrue();
    }

    @Test
    void deserializesTheCreateRequest() throws Exception {
        String json = """
                {
                  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
                  "personalIdentityCode": "010190-123A",
                  "creditRegisterExtractPurposes": ["NewConsumerCredit"]
                }
                """;

        CreateFinancingRequestRequestDto dto = jsonMapper.readValue(
                json,
                CreateFinancingRequestRequestDto.class
        );

        assertThat(dto.creditRegisterExtractPurposes())
                .containsExactly(CreditRegisterExtractPurposeDto.NewConsumerCredit);
    }
}
