package com.creditlens.backend.service;

import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreateFinancingRequestRequest;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.CreditExtractSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterClient;
import com.creditlens.backend.persistence.entity.ConsumerEntity;
import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import com.creditlens.backend.persistence.repository.ConsumerRepository;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class FinancingRequestService {
  private final ConsumerRepository consumerRepository;
  private final FinancingRequestRepository financingRequestRepository;
  private final CreditExtractRepository creditExtractRepository;
  private final PositiveCreditRegisterClient positiveCreditRegisterClient;
  private final TransactionTemplate transactions;
  private final Clock clock;

  public FinancingRequestService(
      ConsumerRepository consumerRepository,
      FinancingRequestRepository financingRequestRepository,
      CreditExtractRepository creditExtractRepository,
      PositiveCreditRegisterClient positiveCreditRegisterClient,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.consumerRepository = consumerRepository;
    this.financingRequestRepository = financingRequestRepository;
    this.creditExtractRepository = creditExtractRepository;
    this.positiveCreditRegisterClient = positiveCreditRegisterClient;
    this.transactions = new TransactionTemplate(transactionManager);
    this.clock = clock;
  }

  public CreateFinancingRequestResponse create(CreateFinancingRequestRequest request) {
    PersonalIdentityCode personalIdentityCode =
        PersonalIdentityCode.of(request.personalIdentityCode());
    List<CreditRegisterExtractPurpose> purposes =
        request.creditRegisterExtractPurposes().stream()
            .map(purpose -> CreditRegisterExtractPurpose.valueOf(purpose.name()))
            .toList();

    var existing = financingRequestRepository.findByClientRequestId(request.clientRequestId());
    if (existing.isPresent()) {
      FinancingRequest saved = getFinancingRequest(existing.orElseThrow());
      if (!saved.hasSameInput(personalIdentityCode, purposes)) {
        throw new ClientRequestConflictException();
      }
      return toResponse(saved, false);
    }

    Instant requestedAt = clock.instant();
    CreditExtract extract =
        positiveCreditRegisterClient.requestCreditExtract(personalIdentityCode, purposes);
    Instant completedAt = clock.instant();
    FinancingRequest saved =
        Objects.requireNonNull(
            transactions.execute(
                status -> {
                  ConsumerEntity consumerEntity =
                      consumerRepository
                          .findByPersonalIdentityCode(personalIdentityCode.value())
                          .orElseGet(
                              () ->
                                  consumerRepository.save(
                                      new ConsumerEntity(
                                          new Consumer(
                                              UUID.randomUUID(),
                                              personalIdentityCode,
                                              requestedAt))));
                  CreditExtract persistedExtract =
                      extract.withPersistenceMetadata(extract.id(), completedAt);
                  FinancingRequest financingRequest =
                      FinancingRequest.create(
                          UUID.randomUUID(),
                          request.clientRequestId(),
                          consumerEntity.toDomain(),
                          purposes,
                          requestedAt,
                          completedAt,
                          persistedExtract);
                  FinancingRequestEntity requestEntity =
                      financingRequestRepository.save(
                          new FinancingRequestEntity(financingRequest, consumerEntity));
                  creditExtractRepository.save(
                      new CreditExtractEntity(persistedExtract, requestEntity));
                  return financingRequest;
                }));
    return toResponse(saved, true);
  }

  private FinancingRequest getFinancingRequest(FinancingRequestEntity entity) {
    CreditExtract extract =
        creditExtractRepository
            .findByFinancingRequest_Id(entity.getId())
            .map(CreditExtractEntity::toDomain)
            .orElseThrow(
                () -> new IllegalStateException("saved financing request has no credit extract"));
    return entity.toDomain(extract);
  }

  private CreateFinancingRequestResponse toResponse(
      FinancingRequest request, boolean newlyCreated) {
    CreditExtract extract = request.creditExtract();
    return new CreateFinancingRequestResponse(
        request.id(),
        request.clientRequestId(),
        new ConsumerDto(
            request.consumer().id(), request.consumer().personalIdentityCode().masked()),
        request.extractPurposes().stream()
            .map(p -> CreditRegisterExtractPurposeDto.valueOf(p.name()))
            .toList(),
        request.requestedAt(),
        request.completedAt(),
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
