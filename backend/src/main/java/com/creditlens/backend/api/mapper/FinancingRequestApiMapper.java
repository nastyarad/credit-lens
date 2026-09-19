package com.creditlens.backend.api.mapper;

import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreateFinancingRequestRequestDto;
import com.creditlens.backend.api.dto.CreditExtractSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.FinancingRequestDto;
import com.creditlens.backend.api.dto.FinancingRequestErrorCodeDto;
import com.creditlens.backend.api.dto.FinancingRequestErrorDto;
import com.creditlens.backend.api.dto.FinancingRequestStatusDto;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.application.CreateFinancingRequestCommand;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import org.springframework.stereotype.Component;

@Component
public class FinancingRequestApiMapper {

    public CreateFinancingRequestCommand toCommand(CreateFinancingRequestRequestDto dto) {
        return new CreateFinancingRequestCommand(
                dto.clientRequestId(),
                PersonalIdentityCode.of(dto.personalIdentityCode()),
                dto.creditRegisterExtractPurposes().stream()
                        .map(purpose -> CreditRegisterExtractPurpose.valueOf(purpose.name()))
                        .toList()
        );
    }

    public FinancingRequestDto toDto(FinancingRequest request) {
        return new FinancingRequestDto(
                request.id(),
                request.clientRequestId(),
                new ConsumerDto(
                        request.consumer().id(),
                        request.consumer().personalIdentityCode().masked()
                ),
                request.extractPurposes().stream()
                        .map(purpose -> CreditRegisterExtractPurposeDto.valueOf(purpose.name()))
                        .toList(),
                FinancingRequestStatusDto.valueOf(request.status().name()),
                request.requestedAt(),
                request.completedAt(),
                toErrorDto(request),
                toSummaryDto(request.creditExtract())
        );
    }

    private FinancingRequestErrorDto toErrorDto(FinancingRequest request) {
        if (request.errorCode() == null) {
            return null;
        }
        return new FinancingRequestErrorDto(
                FinancingRequestErrorCodeDto.valueOf(request.errorCode()),
                request.errorMessage()
        );
    }

    private CreditExtractSummaryDto toSummaryDto(CreditExtract extract) {
        if (extract == null) {
            return null;
        }
        return new CreditExtractSummaryDto(
                extract.extractReference(),
                extract.creationTimeUtc(),
                new VoluntaryBanOnCreditsDto(
                        extract.voluntaryBanOnCredits().isInEffect(),
                        extract.voluntaryBanOnCredits().reason() == null
                                ? null
                                : VoluntaryCreditBanReasonDto.valueOf(
                                        extract.voluntaryBanOnCredits().reason().name()
                                )
                ),
                extract.creditInformationSummary().lendersCount(),
                extract.creditInformationSummary().loanContractsCount(),
                extract.creditInformationSummary().guaranteedLoanContractsCount()
        );
    }
}
