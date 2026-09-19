package com.creditlens.backend.integration.pcr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PcrTransport {
  private PcrTransport() {}

  public record Request(
      String targetEnvironment,
      Owner owner,
      String idCodeType,
      String idCode,
      List<String> creditRegisterExtractPurpose) {}

  public record Owner(String idCodeType, String idCode, String countryCode) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Response(
      CreditRegisterExtract creditRegisterExtract,
      String statusMessage,
      String correlationId,
      List<ErrorResponse> errorResponses) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ErrorResponse(String fieldName, String errorCode, String errorDescription) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CreditRegisterExtract(
      String extractReference,
      String creationTimeUtc,
      PersonRequested personRequested,
      VoluntaryBan voluntaryBanOnCredits,
      Summary creditInformationSummary,
      List<Amount> repaymentsPaidLastAmount,
      List<Amount> sumOfMonthlyLeasingInstalments,
      List<Loan> loans,
      List<Income> incomeData) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PersonRequested(String idCode) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record VoluntaryBan(Boolean isInEffect, String reason) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Summary(
      Integer lendersCount, Integer loanContractsCount, Integer guaranteedLoanContractsCount) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Amount(String currencyCode, BigDecimal sum) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Loan(
      String loanType,
      String contractDate,
      Boolean isLoanWithCollateral,
      List<String> collateralTypes,
      Integer borrowersCount,
      String currencyCode,
      PaymentPlan paymentPlan,
      Boolean accuracyIsDenied,
      LumpSumLoan lumpSumLoan,
      RunningAccountLoan runningAccountLoan,
      LeasingContract leasingContract,
      List<DelayedAmount> delayedAmounts,
      Boolean isForeclosed) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PaymentPlan(
      Boolean isInDebtArrangement, Boolean isInBusinessRestructuringProgram) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LumpSumLoan(
      BigDecimal amountIssued,
      BigDecimal amountPaid,
      BigDecimal balance,
      String plannedFinalDueDate,
      Integer amortizationFrequency) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RunningAccountLoan(
      BigDecimal creditLimit, BigDecimal balance, String balanceDate) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LeasingContract(String contractPeriodStartDate, BigDecimal transactionPrice) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record DelayedAmount(BigDecimal delayedInstalment, String originalDueDate) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Income(Integer year, List<MonthlyIncome> months) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MonthlyIncome(
      Integer month,
      BigDecimal wagesGrossAmount,
      BigDecimal wagesNetAmount,
      BigDecimal benefitsGrossAmount,
      BigDecimal benefitsNetAmount) {}

  static LocalDate date(String value) {
    return value == null ? null : java.time.OffsetDateTime.parse(value).toLocalDate();
  }
}
