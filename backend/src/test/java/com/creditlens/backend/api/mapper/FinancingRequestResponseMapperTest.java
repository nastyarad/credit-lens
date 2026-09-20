package com.creditlens.backend.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.backend.api.dto.CollateralTypeDto;
import com.creditlens.backend.api.dto.CreditExtractDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.LoanTypeDto;
import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditExtractData;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import com.creditlens.backend.domain.VoluntaryCreditBanReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancingRequestResponseMapperTest {
  @Test
  void mapsAndMasksCreateResponseForNewAndExistingRequests() {
    UUID requestId = UUID.randomUUID();
    UUID clientRequestId = UUID.randomUUID();
    UUID extractReference = UUID.randomUUID();
    Instant time = Instant.parse("2026-09-19T10:15:30Z");
    FinancingRequest source =
        FinancingRequest.create(
            requestId,
            clientRequestId,
            new Consumer(UUID.randomUUID(), PersonalIdentityCode.of("010190-123A"), time),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
            time,
            time,
            new CreditExtract(
                UUID.randomUUID(),
                extractReference,
                time,
                new VoluntaryBanOnCredits(true, VoluntaryCreditBanReason.RiskOfIdentityTheft),
                new CreditInformationSummary(2, 3, 1),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                time));

    var created = FinancingRequestResponseMapper.toCreateResponse(source, true);
    var existing = FinancingRequestResponseMapper.toCreateResponse(source, false);

    assertThat(created.id()).isEqualTo(requestId);
    assertThat(created.clientRequestId()).isEqualTo(clientRequestId);
    assertThat(created.consumer().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(created.creditRegisterExtractPurposes())
        .containsExactly(CreditRegisterExtractPurposeDto.NewConsumerCredit);
    assertThat(created.creditExtractSummary().extractReference()).isEqualTo(extractReference);
    assertThat(created.creditExtractSummary().lendersCount()).isEqualTo(2);
    assertThat(created.creditExtractSummary().loanContractsCount()).isEqualTo(3);
    assertThat(created.creditExtractSummary().guaranteedLoanContractsCount()).isEqualTo(1);
    assertThat(created.creditExtractSummary().voluntaryBanOnCredits().reason())
        .isEqualTo(com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto.RiskOfIdentityTheft);
    assertThat(created.newlyCreated()).isTrue();
    assertThat(existing.newlyCreated()).isFalse();
  }

  @Test
  void mapsCompleteDetailsIncludingNestedCreditExtractData() {
    Instant time = Instant.parse("2026-09-19T10:15:30Z");
    CreditExtractData.Loan completeLoan =
        new CreditExtractData.Loan(
            CreditExtractData.LoanType.LumpSumLoan,
            LocalDate.of(2025, 1, 15),
            true,
            List.of(
                CreditExtractData.CollateralType.ApartmentOrRealEstate,
                CreditExtractData.CollateralType.PersonalGuarantee),
            2,
            "EUR",
            new CreditExtractData.PaymentPlan(true, false),
            true,
            new CreditExtractData.LumpSumLoan(
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(1500),
                BigDecimal.valueOf(8500),
                LocalDate.of(2030, 1, 15),
                12),
            new CreditExtractData.RunningAccountLoan(
                BigDecimal.valueOf(5000), BigDecimal.valueOf(1200), LocalDate.of(2026, 9, 1)),
            new CreditExtractData.LeasingContract(
                LocalDate.of(2026, 1, 1), BigDecimal.valueOf(20000)),
            List.of(
                new CreditExtractData.DelayedAmount(
                    BigDecimal.valueOf(100), LocalDate.of(2026, 8, 15), true)));
    CreditExtractData.Loan minimalLoan =
        new CreditExtractData.Loan(
            CreditExtractData.LoanType.GuaranteeReceivable,
            LocalDate.of(2024, 5, 20),
            null,
            List.of(),
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            List.of());
    CreditExtract sourceExtract =
        new CreditExtract(
            UUID.randomUUID(),
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
            time,
            new VoluntaryBanOnCredits(true, VoluntaryCreditBanReason.RiskOfIdentityTheft),
            new CreditInformationSummary(2, 3, 1),
            List.of(new CreditExtractData.CurrencyAmount("EUR", BigDecimal.valueOf(125.50))),
            List.of(new CreditExtractData.CurrencyAmount("EUR", BigDecimal.valueOf(250))),
            List.of(completeLoan, minimalLoan),
            List.of(
                new CreditExtractData.IncomeData(
                    2026,
                    List.of(
                        new CreditExtractData.MonthlyIncome(
                            8,
                            BigDecimal.valueOf(4000),
                            BigDecimal.valueOf(3000),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO)))),
            time);
    FinancingRequest source =
        FinancingRequest.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new Consumer(UUID.randomUUID(), PersonalIdentityCode.of("010190-123A"), time),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
            time,
            time,
            sourceExtract);

    CreditExtractDto result =
        FinancingRequestResponseMapper.toDetailsResponse(source).creditExtract();

    assertThat(result.extractReference())
        .isEqualTo(UUID.fromString("55555555-5555-5555-5555-555555555555"));
    assertThat(result.voluntaryBanOnCredits().reason())
        .isEqualTo(com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto.RiskOfIdentityTheft);
    assertThat(result.creditInformationSummary().repaymentsPaidLastAmount())
        .extracting("currencyCode", "sum")
        .containsExactly(org.assertj.core.groups.Tuple.tuple("EUR", BigDecimal.valueOf(125.50)));
    assertThat(result.creditInformationSummary().sumOfMonthlyLeasingInstalments())
        .extracting("currencyCode", "sum")
        .containsExactly(org.assertj.core.groups.Tuple.tuple("EUR", BigDecimal.valueOf(250)));

    var mappedCompleteLoan = result.loans().getFirst();
    assertThat(mappedCompleteLoan.loanType()).isEqualTo(LoanTypeDto.LumpSumLoan);
    assertThat(mappedCompleteLoan.collateralType())
        .containsExactly(
            CollateralTypeDto.ApartmentOrRealEstate, CollateralTypeDto.PersonalGuarantee);
    assertThat(mappedCompleteLoan.paymentPlan().isInDebtArrangement()).isTrue();
    assertThat(mappedCompleteLoan.lumpSumLoan().balance()).isEqualByComparingTo("8500");
    assertThat(mappedCompleteLoan.runningAccountLoan().balance()).isEqualByComparingTo("1200");
    assertThat(mappedCompleteLoan.leasingContract().transactionPrice())
        .isEqualByComparingTo("20000");
    assertThat(mappedCompleteLoan.delayedAmount().getFirst().isForeclosed()).isTrue();

    var mappedMinimalLoan = result.loans().get(1);
    assertThat(mappedMinimalLoan.loanType()).isEqualTo(LoanTypeDto.GuaranteeReceivable);
    assertThat(mappedMinimalLoan.paymentPlan()).isNull();
    assertThat(mappedMinimalLoan.lumpSumLoan()).isNull();
    assertThat(mappedMinimalLoan.runningAccountLoan()).isNull();
    assertThat(mappedMinimalLoan.leasingContract()).isNull();
    assertThat(result.incomeData().getFirst().months().getFirst().wagesNetAmount())
        .isEqualByComparingTo("3000");
  }
}
