package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LeasingContractDtoTest {
  @Test
  void createsLeasingContract() {
    LeasingContractDto dto = new LeasingContractDto(LocalDate.EPOCH, BigDecimal.TEN);

    assertThat(dto.transactionPrice()).isEqualTo(BigDecimal.TEN);
  }
}
