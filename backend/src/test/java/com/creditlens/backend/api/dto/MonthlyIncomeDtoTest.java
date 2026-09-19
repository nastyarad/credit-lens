package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MonthlyIncomeDtoTest {
  @Test
  void createsMonthlyIncome() {
    MonthlyIncomeDto dto =
        new MonthlyIncomeDto(9, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);

    assertThat(dto.month()).isEqualTo(9);
    assertThat(dto.wagesGrossAmount()).isEqualTo(BigDecimal.TEN);
  }
}
