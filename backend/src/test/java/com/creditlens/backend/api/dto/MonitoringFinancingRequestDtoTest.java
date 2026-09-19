package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitoringFinancingRequestDtoTest {
  @Test
  void createsMonitoringRequest() {
    MonitoringFinancingRequestDto dto =
        new MonitoringFinancingRequestDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.MIN,
            Instant.MAX,
            "******-123A",
            VoluntaryCreditBanReasonDto.Other,
            1,
            2,
            3);

    assertThat(dto.lendersCount()).isEqualTo(1);
    assertThat(dto.loanContractsCount()).isEqualTo(2);
    assertThat(dto.guaranteedLoanContractsCount()).isEqualTo(3);
  }
}
