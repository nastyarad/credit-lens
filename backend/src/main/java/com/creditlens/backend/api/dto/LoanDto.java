package com.creditlens.backend.api.dto;

import java.time.LocalDate;
import java.util.List;

public record LoanDto(
        LoanTypeDto loanType,
        LocalDate contractDate,
        Boolean isLoanWithCollateral,
        List<CollateralTypeDto> collateralType,
        Integer borrowersCount,
        String currencyCode,
        PaymentPlanDto paymentPlan,
        boolean accuracyIsDenied,
        LumpSumLoanDto lumpSumLoan,
        RunningAccountLoanDto runningAccountLoan,
        LeasingContractDto leasingContract,
        List<DelayedAmountDto> delayedAmount
) {

    public LoanDto {
        collateralType = List.copyOf(collateralType);
        delayedAmount = List.copyOf(delayedAmount);
    }
}
