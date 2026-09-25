package com.creditlens.backend.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SearchFinancingRequestsRequest(
    @NotBlank @Pattern(regexp = ApiValidation.PERSONAL_IDENTITY_CODE_PATTERN)
        String personalIdentityCode,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size) {

  public SearchFinancingRequestsRequest {
    page = page == null ? 0 : page;
    size = size == null ? 20 : size;
  }

  @Override
  public String toString() {
    return "SearchFinancingRequestsRequest[personalIdentityCode=<redacted>"
        + ", page="
        + page
        + ", size="
        + size
        + "]";
  }
}
