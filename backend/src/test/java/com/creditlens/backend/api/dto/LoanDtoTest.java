package com.creditlens.backend.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoanDtoTest {
  @Test
  void createsLoanWithNestedDetails() {
    LoanDto dto =
        new LoanDto(
            LoanTypeDto.LumpSumLoan,
            LocalDate.of(2026, 1, 1),
            true,
            List.of(CollateralTypeDto.ApartmentOrRealEstate),
            1,
            "EUR",
            null,
            false,
            null,
            null,
            null,
            List.of());

    assertThat(dto.loanType()).isEqualTo(LoanTypeDto.LumpSumLoan);
    assertThat(dto.collateralType()).containsExactly(CollateralTypeDto.ApartmentOrRealEstate);
    assertThat(dto.delayedAmount()).isEmpty();
  }
}
