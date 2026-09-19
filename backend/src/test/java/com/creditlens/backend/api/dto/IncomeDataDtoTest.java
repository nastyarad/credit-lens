package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class IncomeDataDtoTest {
  @Test
  void keepsMonthlyIncomeData() {
    MonthlyIncomeDto month =
        new MonthlyIncomeDto(9, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
    IncomeDataDto dto = new IncomeDataDto(2026, List.of(month));

    assertThat(dto.year()).isEqualTo(2026);
    assertThat(dto.months()).containsExactly(month);
  }
}
