package com.creditlens.backend.api.mapper;

import com.creditlens.backend.api.dto.CollateralTypeDto;
import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.CreditExtractDto;
import com.creditlens.backend.api.dto.CreditExtractSummaryDto;
import com.creditlens.backend.api.dto.CreditInformationSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.CurrencyAmountDto;
import com.creditlens.backend.api.dto.DelayedAmountDto;
import com.creditlens.backend.api.dto.FinancingRequestDetailsDto;
import com.creditlens.backend.api.dto.IncomeDataDto;
import com.creditlens.backend.api.dto.LeasingContractDto;
import com.creditlens.backend.api.dto.LoanDto;
import com.creditlens.backend.api.dto.LoanTypeDto;
import com.creditlens.backend.api.dto.LumpSumLoanDto;
import com.creditlens.backend.api.dto.MonthlyIncomeDto;
import com.creditlens.backend.api.dto.PaymentPlanDto;
import com.creditlens.backend.api.dto.RunningAccountLoanDto;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditExtractData;
import com.creditlens.backend.domain.FinancingRequest;

public final class FinancingRequestResponseMapper {
  private FinancingRequestResponseMapper() {}

  public static CreateFinancingRequestResponse toCreateResponse(
      FinancingRequest source, boolean newlyCreated) {
    CreditExtract extract = source.creditExtract();
    return new CreateFinancingRequestResponse(
        source.id(),
        source.clientRequestId(),
        new ConsumerDto(source.consumer().id(), source.consumer().personalIdentityCode().masked()),
        source.extractPurposes().stream()
            .map(purpose -> CreditRegisterExtractPurposeDto.valueOf(purpose.name()))
            .toList(),
        source.requestedAt(),
        source.completedAt(),
        new CreditExtractSummaryDto(
            extract.extractReference(),
            extract.creationTimeUtc(),
            new VoluntaryBanOnCreditsDto(
                extract.voluntaryBanOnCredits().isInEffect(),
                extract.voluntaryBanOnCredits().reason() == null
                    ? null
                    : VoluntaryCreditBanReasonDto.valueOf(
                        extract.voluntaryBanOnCredits().reason().name())),
            extract.creditInformationSummary().lendersCount(),
            extract.creditInformationSummary().loanContractsCount(),
            extract.creditInformationSummary().guaranteedLoanContractsCount()),
        newlyCreated);
  }

  public static FinancingRequestDetailsDto toDetailsResponse(FinancingRequest source) {
    return new FinancingRequestDetailsDto(
        source.id(),
        source.clientRequestId(),
        new ConsumerDto(source.consumer().id(), source.consumer().personalIdentityCode().masked()),
        source.extractPurposes().stream()
            .map(purpose -> CreditRegisterExtractPurposeDto.valueOf(purpose.name()))
            .toList(),
        source.requestedAt(),
        source.completedAt(),
        toCreditExtract(source.creditExtract()));
  }

  private static CreditExtractDto toCreditExtract(CreditExtract extract) {
    return new CreditExtractDto(
        extract.extractReference(),
        extract.creationTimeUtc(),
        new VoluntaryBanOnCreditsDto(
            extract.voluntaryBanOnCredits().isInEffect(),
            extract.voluntaryBanOnCredits().reason() == null
                ? null
                : VoluntaryCreditBanReasonDto.valueOf(
                    extract.voluntaryBanOnCredits().reason().name())),
        new CreditInformationSummaryDto(
            extract.creditInformationSummary().lendersCount(),
            extract.creditInformationSummary().loanContractsCount(),
            extract.creditInformationSummary().guaranteedLoanContractsCount(),
            extract.repaymentsPaidLastAmount().stream()
                .map(FinancingRequestResponseMapper::toCurrencyAmount)
                .toList(),
            extract.sumOfMonthlyLeasingInstalments().stream()
                .map(FinancingRequestResponseMapper::toCurrencyAmount)
                .toList()),
        extract.loans().stream().map(FinancingRequestResponseMapper::toLoan).toList(),
        extract.incomeData().stream().map(FinancingRequestResponseMapper::toIncomeData).toList());
  }

  private static CurrencyAmountDto toCurrencyAmount(CreditExtractData.CurrencyAmount source) {
    return new CurrencyAmountDto(source.currencyCode(), source.sum());
  }

  private static LoanDto toLoan(CreditExtractData.Loan source) {
    return new LoanDto(
        LoanTypeDto.valueOf(source.loanType().name()),
        source.contractDate(),
        source.isLoanWithCollateral(),
        source.collateralType().stream()
            .map(type -> CollateralTypeDto.valueOf(type.name()))
            .toList(),
        source.borrowersCount(),
        source.currencyCode(),
        source.paymentPlan() == null
            ? null
            : new PaymentPlanDto(
                source.paymentPlan().isInDebtArrangement(),
                source.paymentPlan().isInBusinessRestructuringProgram()),
        source.accuracyIsDenied(),
        source.lumpSumLoan() == null ? null : toLumpSumLoan(source.lumpSumLoan()),
        source.runningAccountLoan() == null
            ? null
            : toRunningAccountLoan(source.runningAccountLoan()),
        source.leasingContract() == null ? null : toLeasingContract(source.leasingContract()),
        source.delayedAmount().stream()
            .map(FinancingRequestResponseMapper::toDelayedAmount)
            .toList());
  }

  private static LumpSumLoanDto toLumpSumLoan(CreditExtractData.LumpSumLoan source) {
    return new LumpSumLoanDto(
        source.amountIssued(),
        source.amountPaid(),
        source.balance(),
        source.plannedFinalDueDate(),
        source.amortizationFrequency());
  }

  private static RunningAccountLoanDto toRunningAccountLoan(
      CreditExtractData.RunningAccountLoan source) {
    return new RunningAccountLoanDto(source.creditLimit(), source.balance(), source.balanceDate());
  }

  private static LeasingContractDto toLeasingContract(CreditExtractData.LeasingContract source) {
    return new LeasingContractDto(source.contractPeriodStartDate(), source.transactionPrice());
  }

  private static DelayedAmountDto toDelayedAmount(CreditExtractData.DelayedAmount source) {
    return new DelayedAmountDto(
        source.delayedInstalment(), source.originalDueDate(), source.isForeclosed());
  }

  private static IncomeDataDto toIncomeData(CreditExtractData.IncomeData source) {
    return new IncomeDataDto(
        source.year(),
        source.months().stream()
            .map(
                month ->
                    new MonthlyIncomeDto(
                        month.month(),
                        month.wagesGrossAmount(),
                        month.wagesNetAmount(),
                        month.benefitsGrossAmount(),
                        month.benefitsNetAmount()))
            .toList());
  }
}
