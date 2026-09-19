package com.creditlens.backend.api.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DtoSerializationTest {

    @Autowired
    JsonMapper jsonMapper;

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

        assertJsonEquals(dto, """
                {
                  "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
                  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
                  "consumer": {
                    "id": "d2fbb47f-2317-4740-8fa5-50f73b64182d",
                    "maskedPersonalIdentityCode": "******-123A"
                  },
                  "creditRegisterExtractPurposes": ["NewConsumerCredit"],
                  "status": "COMPLETED",
                  "requestedAt": "2026-09-18T10:15:29Z",
                  "completedAt": "2026-09-18T10:15:30Z",
                  "error": null,
                  "creditExtract": {
                    "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
                    "creationTimeUtc": "2026-09-18T10:15:30Z",
                    "voluntaryBanOnCredits": {
                      "isInEffect": true,
                      "reason": "ControlOfPersonalFinances"
                    },
                    "creditInformationSummary": {
                      "lendersCount": 2,
                      "loanContractsCount": 3,
                      "guaranteedLoanContractsCount": 0,
                      "repaymentsPaidLastAmount": [{"currencyCode": "EUR", "sum": 450.00}],
                      "sumOfMonthlyLeasingInstalments": []
                    },
                    "loans": [{
                      "loanType": "LumpSumLoan",
                      "contractDate": "2024-01-15",
                      "isLoanWithCollateral": false,
                      "collateralType": [],
                      "borrowersCount": 1,
                      "currencyCode": "EUR",
                      "paymentPlan": {
                        "isInDebtArrangement": false,
                        "isInBusinessRestructuringProgram": false
                      },
                      "accuracyIsDenied": false,
                      "lumpSumLoan": {
                        "amountIssued": 5000.00,
                        "amountPaid": 1800.00,
                        "balance": 3200.00,
                        "plannedFinalDueDate": "2028-01-15",
                        "amortizationFrequency": 1
                      },
                      "runningAccountLoan": null,
                      "leasingContract": null,
                      "delayedAmount": []
                    }],
                    "incomeData": []
                  }
                }
                """);
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

    @Test
    void serializesSummaryHistoryMonitoringAndProblemPayloads() throws Exception {
        UUID requestId = UUID.fromString("d46c2b84-d979-43e2-b0e4-bc7de12d2454");
        UUID clientRequestId = UUID.fromString("ec2364ec-b4ed-4af7-805c-2bd44c42b9d5");
        UUID consumerId = UUID.fromString("d2fbb47f-2317-4740-8fa5-50f73b64182d");
        UUID extractReference = UUID.fromString("2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10");
        Instant requestedAt = Instant.parse("2026-09-18T10:15:29Z");
        Instant completedAt = Instant.parse("2026-09-18T10:15:30Z");

        FinancingRequestDto failed = new FinancingRequestDto(
                requestId,
                clientRequestId,
                new ConsumerDto(consumerId, "******-123A"),
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
                FinancingRequestStatusDto.FAILED,
                requestedAt,
                completedAt,
                new FinancingRequestErrorDto(FinancingRequestErrorCodeDto.PCR_TIMEOUT, null),
                null
        );
        assertJsonEquals(failed, """
                {
                  "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
                  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
                  "consumer": {
                    "id": "d2fbb47f-2317-4740-8fa5-50f73b64182d",
                    "maskedPersonalIdentityCode": "******-123A"
                  },
                  "creditRegisterExtractPurposes": ["NewConsumerCredit"],
                  "status": "FAILED",
                  "requestedAt": "2026-09-18T10:15:29Z",
                  "completedAt": "2026-09-18T10:15:30Z",
                  "error": {
                    "code": "PCR_TIMEOUT",
                    "message": "The Positive Credit Register did not respond in time."
                  },
                  "creditExtractSummary": null
                }
                """);

        PageDto<FinancingRequestHistoryItemDto> history = new PageDto<>(
                List.of(new FinancingRequestHistoryItemDto(
                        requestId,
                        clientRequestId,
                        "******-123A",
                        FinancingRequestStatusDto.COMPLETED,
                        requestedAt,
                        completedAt,
                        extractReference,
                        true
                )),
                0,
                20,
                1,
                1
        );
        assertJsonEquals(history, """
                {
                  "items": [{
                    "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
                    "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
                    "maskedPersonalIdentityCode": "******-123A",
                    "status": "COMPLETED",
                    "requestedAt": "2026-09-18T10:15:29Z",
                    "completedAt": "2026-09-18T10:15:30Z",
                    "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
                    "voluntaryCreditBanActive": true
                  }],
                  "page": 0,
                  "size": 20,
                  "totalItems": 1,
                  "totalPages": 1
                }
                """);

        PageDto<MonitoringFinancingRequestDto> monitoring = new PageDto<>(
                List.of(new MonitoringFinancingRequestDto(
                        requestId,
                        extractReference,
                        requestedAt,
                        completedAt,
                        "******-123A",
                        VoluntaryCreditBanReasonDto.ControlOfPersonalFinances,
                        2,
                        3,
                        0
                )),
                0,
                100,
                1,
                1
        );
        assertJsonEquals(monitoring, """
                {
                  "items": [{
                    "financingRequestId": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
                    "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
                    "requestedAt": "2026-09-18T10:15:29Z",
                    "completedAt": "2026-09-18T10:15:30Z",
                    "maskedPersonalIdentityCode": "******-123A",
                    "voluntaryCreditBanReason": "ControlOfPersonalFinances",
                    "lendersCount": 2,
                    "loanContractsCount": 3,
                    "guaranteedLoanContractsCount": 0
                  }],
                  "page": 0,
                  "size": 100,
                  "totalItems": 1,
                  "totalPages": 1
                }
                """);

        ApiProblemDto problem = new ApiProblemDto(
                URI.create("https://credit-lens.local/problems/invalid-personal-identity-code"),
                "Invalid personal identity code",
                400,
                "The supplied personal identity code has an invalid format.",
                URI.create("/api/v1/financing-requests"),
                UUID.fromString("0b1bc2e7-8f84-47dd-8326-a8bfef0e08af")
        );
        assertJsonEquals(problem, """
                {
                  "type": "https://credit-lens.local/problems/invalid-personal-identity-code",
                  "title": "Invalid personal identity code",
                  "status": 400,
                  "detail": "The supplied personal identity code has an invalid format.",
                  "instance": "/api/v1/financing-requests",
                  "correlationId": "0b1bc2e7-8f84-47dd-8326-a8bfef0e08af"
                }
                """);
    }

    private void assertJsonEquals(Object actual, String expected) throws Exception {
        JsonNode actualJson = jsonMapper.readTree(jsonMapper.writeValueAsString(actual));
        assertThat(actualJson).isEqualTo(jsonMapper.readTree(expected));
    }
}
