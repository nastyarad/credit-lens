package com.creditlens.backend.api.mapper;

import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.CreditExtractSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.domain.CreditExtract;
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
}
