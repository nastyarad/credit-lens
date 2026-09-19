package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CurrencyAmountDtoTest {
  @Test
  void createsCurrencyAmount() {
    CurrencyAmountDto dto = new CurrencyAmountDto("EUR", BigDecimal.TEN);

    assertThat(dto.currencyCode()).isEqualTo("EUR");
    assertThat(dto.sum()).isEqualTo(BigDecimal.TEN);
  }
}
