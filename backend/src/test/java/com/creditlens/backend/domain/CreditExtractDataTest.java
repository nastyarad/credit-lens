package com.creditlens.backend.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreditExtractDataTest {
  @Test
  void createsCreditExtractDataRecordsAndKeepsCollectionsImmutable() {
    CreditExtractData.CurrencyAmount amount =
        new CreditExtractData.CurrencyAmount("EUR", BigDecimal.TEN);
    CreditExtractData.PaymentPlan paymentPlan = new CreditExtractData.PaymentPlan(true, false);
    CreditExtractData.LumpSumLoan lumpSumLoan =
        new CreditExtractData.LumpSumLoan(
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(80),
            LocalDate.of(2030, 1, 1),
            12);
    CreditExtractData.RunningAccountLoan runningAccountLoan =
        new CreditExtractData.RunningAccountLoan(
            BigDecimal.valueOf(500), BigDecimal.valueOf(100), LocalDate.of(2026, 9, 1));
    CreditExtractData.LeasingContract leasingContract =
        new CreditExtractData.LeasingContract(LocalDate.of(2025, 1, 1), BigDecimal.valueOf(20000));
    CreditExtractData.DelayedAmount delayedAmount =
        new CreditExtractData.DelayedAmount(
            BigDecimal.valueOf(10), LocalDate.of(2026, 8, 1), false);
    CreditExtractData.Loan loan =
        new CreditExtractData.Loan(
            CreditExtractData.LoanType.LumpSumLoan,
            LocalDate.of(2025, 1, 1),
            true,
            List.of(CreditExtractData.CollateralType.ApartmentOrRealEstate),
            1,
            "EUR",
            paymentPlan,
            false,
            lumpSumLoan,
            runningAccountLoan,
            leasingContract,
            List.of(delayedAmount));
    CreditExtractData.MonthlyIncome income =
        new CreditExtractData.MonthlyIncome(
            9,
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(2400),
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    CreditExtractData.IncomeData incomeData =
        new CreditExtractData.IncomeData(2026, List.of(income));

    assertThat(amount.currencyCode()).isEqualTo("EUR");
    assertThat(amount.sum()).isEqualTo(BigDecimal.TEN);
    assertThat(loan.loanType()).isEqualTo(CreditExtractData.LoanType.LumpSumLoan);
    assertThat(loan.lumpSumLoan()).isEqualTo(lumpSumLoan);
    assertThat(loan.runningAccountLoan()).isEqualTo(runningAccountLoan);
    assertThat(loan.leasingContract()).isEqualTo(leasingContract);
    assertThat(loan.delayedAmount()).containsExactly(delayedAmount);
    assertThat(incomeData.months()).containsExactly(income);
    assertThatThrownBy(
            () -> loan.collateralType().add(CreditExtractData.CollateralType.OtherCollateral))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> incomeData.months().add(income))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
