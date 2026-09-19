package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
    FinancingRequestSearchRequestDto search =
        new FinancingRequestSearchRequestDto(personalIdentityCode, 0, 20);
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
}
