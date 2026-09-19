package com.creditlens.backend.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CreditExtractData {

  private CreditExtractData() {}

  public record CurrencyAmount(String currencyCode, BigDecimal sum) {}

  public enum LoanType {
    LumpSumLoan,
    RunningAccountLoan,
    Leasing,
    GuaranteeReceivable
  }

  public enum CollateralType {
    ApartmentOrRealEstate,
    OtherImmovableProperty,
    InstalmentSaleItem,
    OtherMoveableProperty,
    OtherCollateral,
    PersonalGuarantee,
    GovernmentGuarantee,
    OtherGuarantee
  }

  public record PaymentPlan(
      boolean isInDebtArrangement, boolean isInBusinessRestructuringProgram) {}

  public record LumpSumLoan(
      BigDecimal amountIssued,
      BigDecimal amountPaid,
      BigDecimal balance,
      LocalDate plannedFinalDueDate,
      Integer amortizationFrequency) {}

  public record RunningAccountLoan(
      BigDecimal creditLimit, BigDecimal balance, LocalDate balanceDate) {}

  public record LeasingContract(LocalDate contractPeriodStartDate, BigDecimal transactionPrice) {}

  public record DelayedAmount(
      BigDecimal delayedInstalment, LocalDate originalDueDate, boolean isForeclosed) {}

  public record Loan(
      LoanType loanType,
      LocalDate contractDate,
      Boolean isLoanWithCollateral,
      List<CollateralType> collateralType,
      Integer borrowersCount,
      String currencyCode,
      PaymentPlan paymentPlan,
      boolean accuracyIsDenied,
      LumpSumLoan lumpSumLoan,
      RunningAccountLoan runningAccountLoan,
      LeasingContract leasingContract,
      List<DelayedAmount> delayedAmount) {
    public Loan {
      collateralType = List.copyOf(collateralType);
      delayedAmount = List.copyOf(delayedAmount);
    }
  }

  public record MonthlyIncome(
      int month,
      BigDecimal wagesGrossAmount,
      BigDecimal wagesNetAmount,
      BigDecimal benefitsGrossAmount,
      BigDecimal benefitsNetAmount) {}

  public record IncomeData(int year, List<MonthlyIncome> months) {
    public IncomeData {
      months = List.copyOf(months);
    }
  }
}
