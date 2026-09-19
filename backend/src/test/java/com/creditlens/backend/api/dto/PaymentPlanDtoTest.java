package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentPlanDtoTest {
  @Test
  void createsPaymentPlan() {
    PaymentPlanDto dto = new PaymentPlanDto(true, false);

    assertThat(dto.isInDebtArrangement()).isTrue();
    assertThat(dto.isInBusinessRestructuringProgram()).isFalse();
  }
}
