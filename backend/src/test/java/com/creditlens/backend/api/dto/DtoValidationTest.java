package com.creditlens.backend.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
