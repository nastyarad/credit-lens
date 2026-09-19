package com.creditlens.backend.integration.pcr;

import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditExtractData;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import com.creditlens.backend.domain.VoluntaryCreditBanReason;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class PcrCreditExtractMapper {
  private PcrCreditExtractMapper() {}

  static CreditExtract map(
      PcrTransport.CreditRegisterExtract source, PersonalIdentityCode requested) {
    try {
      if (source == null
          || source.personRequested() == null
          || !requested.value().equals(source.personRequested().idCode())) {
        throw new IllegalArgumentException("personRequested mismatch");
      }
      UUID reference = UUID.fromString(source.extractReference());
      Instant created = Instant.parse(source.creationTimeUtc());
      PcrTransport.Summary summary = source.creditInformationSummary();
      if (summary == null) throw new IllegalArgumentException("summary missing");
      CreditInformationSummary mappedSummary =
          new CreditInformationSummary(
              requiredNonNegative(summary.lendersCount()),
              requiredNonNegative(summary.loanContractsCount()),
              requiredNonNegative(summary.guaranteedLoanContractsCount()));
      PcrTransport.VoluntaryBan ban = source.voluntaryBanOnCredits();
      VoluntaryBanOnCredits mappedBan =
          ban == null
              ? new VoluntaryBanOnCredits(false, null)
              : new VoluntaryBanOnCredits(
                  Boolean.TRUE.equals(ban.isInEffect()),
                  ban.reason() == null ? null : VoluntaryCreditBanReason.valueOf(ban.reason()));
      return new CreditExtract(
          UUID.randomUUID(),
          reference,
          created,
          mappedBan,
          mappedSummary,
          amounts(source.repaymentsPaidLastAmount()),
          amounts(source.sumOfMonthlyLeasingInstalments()),
          loans(source.loans()),
          incomes(source.incomeData()),
          null);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("invalid PCR credit extract", exception);
    }
  }

  private static int requiredNonNegative(Integer value) {
    if (value == null || value < 0) throw new IllegalArgumentException("invalid summary count");
    return value;
  }

  private static List<CreditExtractData.CurrencyAmount> amounts(List<PcrTransport.Amount> values) {
    return values == null
        ? List.of()
        : values.stream()
            .map(value -> new CreditExtractData.CurrencyAmount(value.currencyCode(), value.sum()))
            .toList();
  }

  private static List<CreditExtractData.Loan> loans(List<PcrTransport.Loan> values) {
    return values == null ? List.of() : values.stream().map(PcrCreditExtractMapper::loan).toList();
  }

  private static CreditExtractData.Loan loan(PcrTransport.Loan value) {
    CreditExtractData.LumpSumLoan lump =
        value.lumpSumLoan() == null
            ? null
            : new CreditExtractData.LumpSumLoan(
                value.lumpSumLoan().amountIssued(),
                value.lumpSumLoan().amountPaid(),
                value.lumpSumLoan().balance(),
                PcrTransport.date(value.lumpSumLoan().plannedFinalDueDate()),
                value.lumpSumLoan().amortizationFrequency());
    CreditExtractData.RunningAccountLoan running =
        value.runningAccountLoan() == null
            ? null
            : new CreditExtractData.RunningAccountLoan(
                value.runningAccountLoan().creditLimit(),
                value.runningAccountLoan().balance(),
                PcrTransport.date(value.runningAccountLoan().balanceDate()));
    CreditExtractData.LeasingContract leasing =
        value.leasingContract() == null
            ? null
            : new CreditExtractData.LeasingContract(
                PcrTransport.date(value.leasingContract().contractPeriodStartDate()),
                value.leasingContract().transactionPrice());
    List<CreditExtractData.DelayedAmount> delayed =
        value.delayedAmounts() == null
            ? List.of()
            : value.delayedAmounts().stream()
                .map(
                    item ->
                        new CreditExtractData.DelayedAmount(
                            item.delayedInstalment(),
                            PcrTransport.date(item.originalDueDate()),
                            Boolean.TRUE.equals(value.isForeclosed())))
                .toList();
    return new CreditExtractData.Loan(
        CreditExtractData.LoanType.valueOf(value.loanType()),
        PcrTransport.date(value.contractDate()),
        value.isLoanWithCollateral(),
        collateralTypes(value.collateralTypes()),
        value.borrowersCount(),
        value.currencyCode(),
        value.paymentPlan() == null
            ? null
            : new CreditExtractData.PaymentPlan(
                Boolean.TRUE.equals(value.paymentPlan().isInDebtArrangement()),
                Boolean.TRUE.equals(value.paymentPlan().isInBusinessRestructuringProgram())),
        Boolean.TRUE.equals(value.accuracyIsDenied()),
        lump,
        running,
        leasing,
        delayed);
  }

  private static List<CreditExtractData.CollateralType> collateralTypes(List<String> values) {
    return values == null
        ? List.of()
        : values.stream().map(CreditExtractData.CollateralType::valueOf).toList();
  }

  private static List<CreditExtractData.IncomeData> incomes(List<PcrTransport.Income> values) {
    return values == null
        ? List.of()
        : values.stream()
            .map(
                value ->
                    new CreditExtractData.IncomeData(
                        value.year(),
                        value.months() == null
                            ? List.of()
                            : value.months().stream()
                                .map(
                                    month ->
                                        new CreditExtractData.MonthlyIncome(
                                            month.month(),
                                            month.wagesGrossAmount(),
                                            month.wagesNetAmount(),
                                            month.benefitsGrossAmount(),
                                            month.benefitsNetAmount()))
                                .toList()))
            .toList();
  }
}
