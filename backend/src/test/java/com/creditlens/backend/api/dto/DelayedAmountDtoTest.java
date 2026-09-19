package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DelayedAmountDtoTest {
  @Test
  void createsDelayedAmount() {
    DelayedAmountDto dto = new DelayedAmountDto(BigDecimal.ONE, LocalDate.EPOCH, false);

    assertThat(dto.delayedInstalment()).isEqualTo(BigDecimal.ONE);
    assertThat(dto.isForeclosed()).isFalse();
  }
}
