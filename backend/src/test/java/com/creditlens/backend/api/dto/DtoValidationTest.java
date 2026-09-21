package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DtoValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsInvalidCreateRequest() {
    CreateFinancingRequestRequest request =
        new CreateFinancingRequestRequest(UUID.randomUUID(), "not-an-id", List.of());
    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactlyInAnyOrder("personalIdentityCode", "creditRegisterExtractPurposes");
  }

  @Test
  void redactsPersonalIdentityCodesFromStringRepresentations() {
    String personalIdentityCode = "010190-123A";
    CreateFinancingRequestRequest request =
        new CreateFinancingRequestRequest(
            UUID.randomUUID(),
            personalIdentityCode,
            List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit));
    SearchFinancingRequestRequest search =
        new SearchFinancingRequestRequest(personalIdentityCode, 0, 20);
    assertThat(request.toString())
        .contains("personalIdentityCode=<redacted>")
        .doesNotContain(personalIdentityCode);
    assertThat(search.toString())
        .contains("personalIdentityCode=<redacted>")
        .doesNotContain(personalIdentityCode);
  }

  @Test
  void createResponseRequiresCompletedExtract() {
    assertThat(
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    new CreateFinancingRequestResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new ConsumerDto(UUID.randomUUID(), "******-123A"),
                        List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
                        java.time.Instant.EPOCH,
                        java.time.Instant.EPOCH,
                        null,
                        false)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void appliesMonitoringPaginationDefaults() {
    GetMonitoringFinancingRequestsRequest request =
        new GetMonitoringFinancingRequestsRequest(
            Instant.parse("2026-09-18T10:00:00Z"),
            Instant.parse("2026-09-18T10:05:00Z"),
            null,
            null);

    assertThat(request.page()).isZero();
    assertThat(request.size()).isEqualTo(100);
    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void validatesMonitoringIntervalAndPagination() {
    Instant from = Instant.parse("2026-09-18T10:00:00Z");
    Instant to = Instant.parse("2026-09-18T10:05:00Z");

    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, to, -1, 100)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("page");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, to, 0, 0)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("size");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, to, 0, 501)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("size");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(null, to, 0, 100)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("completedFrom");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, null, 0, 100)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("completedTo");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, from, 0, 100)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("intervalValid");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(to, from, 0, 100)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("intervalValid");
    assertThat(validator.validate(new GetMonitoringFinancingRequestsRequest(from, to, 0, 500)))
        .isEmpty();
  }

  @Test
  void copiesMonitoringResponseItemsToAnImmutableListAndRejectsInvalidMetadata() {
    List<MonitoringFinancingRequestDto> items =
        new java.util.ArrayList<>(List.of(monitoringItem()));
    GetMonitoringFinancingRequestsResponse response =
        new GetMonitoringFinancingRequestsResponse(items, 0, 100, 1, 1);

    items.clear();
    assertThat(response.items()).containsExactly(monitoringItem());
    assertThatThrownBy(() -> response.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> new GetMonitoringFinancingRequestsResponse(List.of(), -1, 100, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private MonitoringFinancingRequestDto monitoringItem() {
    return new MonitoringFinancingRequestDto(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        Instant.parse("2026-09-18T10:04:40Z"),
        Instant.parse("2026-09-18T10:04:42Z"),
        "******-123A",
        VoluntaryCreditBanReasonDto.ControlOfPersonalFinances,
        2,
        3,
        0);
  }
}
