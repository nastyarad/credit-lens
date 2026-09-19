package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RunningAccountLoanDtoTest {
  @Test
  void createsRunningAccountLoan() {
    RunningAccountLoanDto dto =
        new RunningAccountLoanDto(BigDecimal.TEN, BigDecimal.ONE, LocalDate.EPOCH);

    assertThat(dto.creditLimit()).isEqualTo(BigDecimal.TEN);
  }
}
