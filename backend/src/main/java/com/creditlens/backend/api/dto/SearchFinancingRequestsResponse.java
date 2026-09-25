package com.creditlens.backend.api.dto;

import java.util.List;

public record SearchFinancingRequestsResponse(
    List<FinancingRequestHistoryItemDto> items,
    int page,
    int size,
    long totalItems,
    int totalPages) {

  public SearchFinancingRequestsResponse {
    items = List.copyOf(items);
    if (page < 0 || size < 1 || totalItems < 0 || totalPages < 0) {
      throw new IllegalArgumentException(
          "page metadata must not be negative and size must be positive");
    }
  }
}
