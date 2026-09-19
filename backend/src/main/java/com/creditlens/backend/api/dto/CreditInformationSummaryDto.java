package com.creditlens.backend.api.dto;

import java.util.List;

public record CreditInformationSummaryDto(
    int lendersCount,
    int loanContractsCount,
    int guaranteedLoanContractsCount,
    List<CurrencyAmountDto> repaymentsPaidLastAmount,
    List<CurrencyAmountDto> sumOfMonthlyLeasingInstalments) {

  public CreditInformationSummaryDto {
    if (lendersCount < 0 || loanContractsCount < 0 || guaranteedLoanContractsCount < 0) {
      throw new IllegalArgumentException("credit information counts must not be negative");
    }
    repaymentsPaidLastAmount = List.copyOf(repaymentsPaidLastAmount);
    sumOfMonthlyLeasingInstalments = List.copyOf(sumOfMonthlyLeasingInstalments);
  }
}
