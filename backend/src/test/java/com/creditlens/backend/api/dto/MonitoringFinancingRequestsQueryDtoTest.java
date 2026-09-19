package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MonitoringFinancingRequestsQueryDtoTest {
  @Test
  void appliesDefaultPaginationAndValidatesInterval() {
    MonitoringFinancingRequestsQueryDto dto =
        new MonitoringFinancingRequestsQueryDto(Instant.EPOCH, Instant.MAX, null, null);

    assertThat(dto.page()).isZero();
    assertThat(dto.size()).isEqualTo(100);
    assertThat(dto.isValidInterval()).isTrue();
  }
}
