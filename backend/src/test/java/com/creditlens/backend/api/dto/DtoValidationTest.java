package com.creditlens.backend.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void appliesHistoryPaginationDefaults() {
        FinancingRequestSearchRequestDto request = new FinancingRequestSearchRequestDto(
                "010190-123A",
                null,
                null
        );

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidCreateRequest() {
        CreateFinancingRequestRequestDto request = new CreateFinancingRequestRequestDto(
                UUID.randomUUID(),
                "not-an-id",
                List.of()
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("personalIdentityCode", "creditRegisterExtractPurposes");
    }

    @Test
    void redactsPersonalIdentityCodesFromStringRepresentations() {
        String personalIdentityCode = "010190-123A";
        CreateFinancingRequestRequestDto createRequest = new CreateFinancingRequestRequestDto(
                UUID.randomUUID(),
                personalIdentityCode,
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit)
        );
        FinancingRequestSearchRequestDto searchRequest = new FinancingRequestSearchRequestDto(
                personalIdentityCode,
                0,
                20
        );

        assertThat(createRequest.toString())
                .contains("personalIdentityCode=<redacted>")
                .doesNotContain(personalIdentityCode);
        assertThat(searchRequest.toString())
                .contains("personalIdentityCode=<redacted>")
                .doesNotContain(personalIdentityCode);
    }

    @Test
    void validatesMonitoringIntervalAndDefaults() {
        Instant end = Instant.parse("2026-09-18T10:05:00Z");
        MonitoringFinancingRequestsQueryDto request = new MonitoringFinancingRequestsQueryDto(
                end,
                end,
                null,
                null
        );

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(100);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("validInterval");
    }

    @Test
    void responseCollectionsAreDefensivelyCopied() {
        List<FinancingRequestHistoryItemDto> items = new java.util.ArrayList<>();
        PageDto<FinancingRequestHistoryItemDto> page = new PageDto<>(items, 0, 20, 0, 0);

        items.add(null);

        assertThat(page.items()).isEmpty();
    }

    @Test
    void suppliesSafeDefaultMessageWhenPersistedFailureHasNoMessage() {
        FinancingRequestErrorDto error = new FinancingRequestErrorDto(
                FinancingRequestErrorCodeDto.PCR_TIMEOUT,
                null
        );

        assertThat(error.message()).isEqualTo("The Positive Credit Register did not respond in time.");
    }

    @Test
    void acceptsEachValidFinancingRequestState() {
        Instant requestedAt = Instant.parse("2026-09-18T10:15:29Z");
        Instant completedAt = Instant.parse("2026-09-18T10:15:30Z");

        FinancingRequestDto inProgress = newFinancingRequest(
                FinancingRequestStatusDto.IN_PROGRESS,
                requestedAt,
                null,
                null,
                null
        );
        FinancingRequestDto completed = newFinancingRequest(
                FinancingRequestStatusDto.COMPLETED,
                requestedAt,
                completedAt,
                null,
                creditExtractSummary(completedAt)
        );
        FinancingRequestDto failed = newFinancingRequest(
                FinancingRequestStatusDto.FAILED,
                requestedAt,
                completedAt,
                new FinancingRequestErrorDto(FinancingRequestErrorCodeDto.PCR_TIMEOUT, null),
                null
        );

        assertThat(inProgress.status()).isEqualTo(FinancingRequestStatusDto.IN_PROGRESS);
        assertThat(completed.status()).isEqualTo(FinancingRequestStatusDto.COMPLETED);
        assertThat(failed.status()).isEqualTo(FinancingRequestStatusDto.FAILED);
    }

    @Test
    void rejectsStateDataThatDoesNotMatchStatus() {
        Instant requestedAt = Instant.parse("2026-09-18T10:15:29Z");
        Instant completedAt = Instant.parse("2026-09-18T10:15:30Z");
        FinancingRequestErrorDto error = new FinancingRequestErrorDto(
                FinancingRequestErrorCodeDto.PCR_TIMEOUT,
                null
        );

        assertThatThrownBy(() -> newFinancingRequest(
                FinancingRequestStatusDto.IN_PROGRESS,
                requestedAt,
                completedAt,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newFinancingRequest(
                FinancingRequestStatusDto.COMPLETED,
                requestedAt,
                completedAt,
                error,
                creditExtractSummary(completedAt)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newFinancingRequest(
                FinancingRequestStatusDto.FAILED,
                requestedAt,
                completedAt,
                error,
                creditExtractSummary(completedAt)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newFinancingRequest(
                FinancingRequestStatusDto.FAILED,
                requestedAt,
                requestedAt.minusSeconds(1),
                error,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static FinancingRequestDto newFinancingRequest(
            FinancingRequestStatusDto status,
            Instant requestedAt,
            Instant completedAt,
            FinancingRequestErrorDto error,
            CreditExtractSummaryDto creditExtractSummary
    ) {
        return new FinancingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new ConsumerDto(UUID.randomUUID(), "******-123A"),
                List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
                status,
                requestedAt,
                completedAt,
                error,
                creditExtractSummary
        );
    }

    private static CreditExtractSummaryDto creditExtractSummary(Instant creationTime) {
        return new CreditExtractSummaryDto(
                UUID.randomUUID(),
                creationTime,
                new VoluntaryBanOnCreditsDto(false, null),
                0,
                0,
                0
        );
    }
}
