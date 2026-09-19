package com.creditlens.backend.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record MonitoringFinancingRequestsQueryDto(
    @NotNull Instant completedFrom,
    @NotNull Instant completedTo,
    @Min(0) Integer page,
    @Min(1) @Max(500) Integer size) {

  public MonitoringFinancingRequestsQueryDto {
    page = page == null ? 0 : page;
    size = size == null ? 100 : size;
  }

  @AssertTrue(message = "completedFrom must be before completedTo")
  public boolean isValidInterval() {
    return completedFrom == null || completedTo == null || completedFrom.isBefore(completedTo);
  }
}
