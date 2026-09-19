package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LumpSumLoanDtoTest {
  @Test
  void createsLumpSumLoan() {
    LumpSumLoanDto dto =
        new LumpSumLoanDto(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, LocalDate.EPOCH, 12);

    assertThat(dto.amountIssued()).isEqualTo(BigDecimal.TEN);
  }
}
