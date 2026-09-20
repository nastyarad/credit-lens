package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchFinancingRequestResponseTest {
  @Test
  void copiesItemsAndValidatesPageMetadata() {
    List<FinancingRequestHistoryItemDto> items = new ArrayList<>();
    SearchFinancingRequestResponse response =
        new SearchFinancingRequestResponse(items, 0, 20, 0, 0);

    items.add(
        new FinancingRequestHistoryItemDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "******-123A",
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH,
            UUID.randomUUID(),
            false));

    assertThat(response.items()).isEmpty();
  }
}
