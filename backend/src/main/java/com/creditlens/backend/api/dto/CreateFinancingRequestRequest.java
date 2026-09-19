package com.creditlens.backend.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public record CreateFinancingRequestRequest(
    @NotNull UUID clientRequestId,
    @NotBlank @Pattern(regexp = ApiValidation.PERSONAL_IDENTITY_CODE_PATTERN)
        String personalIdentityCode,
    @NotEmpty List<@Valid @NotNull CreditRegisterExtractPurposeDto> creditRegisterExtractPurposes) {

  public CreateFinancingRequestRequest {
    creditRegisterExtractPurposes =
        creditRegisterExtractPurposes == null ? null : List.copyOf(creditRegisterExtractPurposes);
  }

  @Override
  public String toString() {
    return "CreateFinancingRequestRequest[clientRequestId="
        + clientRequestId
        + ", personalIdentityCode=<redacted>"
        + ", creditRegisterExtractPurposes="
        + creditRegisterExtractPurposes
        + "]";
  }
}
