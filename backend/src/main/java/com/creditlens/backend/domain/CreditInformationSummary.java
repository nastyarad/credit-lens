package com.creditlens.backend.domain;

public record CreditInformationSummary(
    int lendersCount, int loanContractsCount, int guaranteedLoanContractsCount) {

  public CreditInformationSummary {
    if (lendersCount < 0 || loanContractsCount < 0 || guaranteedLoanContractsCount < 0) {
      throw new IllegalArgumentException("credit information counts must not be negative");
    }
  }
}
