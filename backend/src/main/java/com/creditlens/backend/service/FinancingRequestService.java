package com.creditlens.backend.service;

import com.creditlens.backend.api.dto.CreateFinancingRequestRequest;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.FinancingRequestDetailsDto;
import com.creditlens.backend.api.dto.FinancingRequestHistoryItemDto;
import com.creditlens.backend.api.dto.SearchFinancingRequestRequest;
import com.creditlens.backend.api.dto.SearchFinancingRequestResponse;
import com.creditlens.backend.api.mapper.FinancingRequestResponseMapper;
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
import com.creditlens.backend.persistence.repository.FinancingRequestHistoryProjection;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
      return existingResponse(existing.orElseThrow(), personalIdentityCode, purposes);
    }

    Instant requestedAt = clock.instant();
    CreditExtract extract =
        positiveCreditRegisterClient.requestCreditExtract(personalIdentityCode, purposes);
    Instant completedAt = clock.instant();
    try {
      return FinancingRequestResponseMapper.toCreateResponse(
          saveNewRequest(
              new ValidatedFinancingRequestInput(
                  request.clientRequestId(), personalIdentityCode, purposes),
              new CompletedPcrCall(requestedAt, completedAt, extract)),
          true);
    } catch (DataIntegrityViolationException exception) {
      // A concurrent request can finish after the initial idempotency lookup.
      // Reuse its successful result when the clientRequestId constraint caused the race.
      return financingRequestRepository
          .findByClientRequestId(request.clientRequestId())
          .map(entity -> existingResponse(entity, personalIdentityCode, purposes))
          .orElseThrow(() -> exception);
    }
  }

  private FinancingRequest saveNewRequest(
      ValidatedFinancingRequestInput input, CompletedPcrCall pcrCall) {
    return Objects.requireNonNull(
        transactions.execute(
            status -> {
              ConsumerEntity consumer =
                  findOrCreateConsumer(input.personalIdentityCode(), pcrCall.requestedAt());
              CreditExtract persistedExtract =
                  pcrCall
                      .creditExtract()
                      .withPersistenceMetadata(pcrCall.creditExtract().id(), pcrCall.completedAt());
              FinancingRequest financingRequest =
                  FinancingRequest.create(
                      UUID.randomUUID(),
                      input.clientRequestId(),
                      consumer.toDomain(),
                      input.purposes(),
                      pcrCall.requestedAt(),
                      pcrCall.completedAt(),
                      persistedExtract);
              FinancingRequestEntity requestEntity =
                  financingRequestRepository.save(
                      new FinancingRequestEntity(financingRequest, consumer));
              creditExtractRepository.save(
                  new CreditExtractEntity(persistedExtract, requestEntity));
              return financingRequest;
            }));
  }

  private ConsumerEntity findOrCreateConsumer(
      PersonalIdentityCode personalIdentityCode, Instant requestedAt) {
    return consumerRepository
        .findByPersonalIdentityCode(personalIdentityCode.value())
        .orElseGet(
            () ->
                consumerRepository.save(
                    new ConsumerEntity(
                        new Consumer(UUID.randomUUID(), personalIdentityCode, requestedAt))));
  }

  public SearchFinancingRequestResponse searchHistory(SearchFinancingRequestRequest request) {
    PersonalIdentityCode personalIdentityCode =
        PersonalIdentityCode.of(request.personalIdentityCode());
    Page<FinancingRequestHistoryProjection> history =
        financingRequestRepository.findHistoryByPersonalIdentityCode(
            personalIdentityCode.value(), PageRequest.of(request.page(), request.size()));
    return new SearchFinancingRequestResponse(
        history.getContent().stream().map(this::toHistoryItem).toList(),
        history.getNumber(),
        history.getSize(),
        history.getTotalElements(),
        history.getTotalPages());
  }

  public FinancingRequestDetailsDto getDetails(UUID id) {
    FinancingRequestEntity entity =
        financingRequestRepository
            .findById(id)
            .orElseThrow(() -> new FinancingRequestNotFoundException(id));
    return FinancingRequestResponseMapper.toDetailsResponse(getFinancingRequest(entity));
  }

  private FinancingRequestHistoryItemDto toHistoryItem(FinancingRequestHistoryProjection item) {
    return new FinancingRequestHistoryItemDto(
        item.getId(),
        item.getClientRequestId(),
        PersonalIdentityCode.of(item.getPersonalIdentityCode()).masked(),
        item.getRequestedAt(),
        item.getCompletedAt(),
        item.getExtractReference(),
        item.isVoluntaryCreditBanActive());
  }

  private CreateFinancingRequestResponse existingResponse(
      FinancingRequestEntity entity,
      PersonalIdentityCode personalIdentityCode,
      List<CreditRegisterExtractPurpose> purposes) {
    FinancingRequest saved = getFinancingRequest(entity);
    if (!saved.hasSameInput(personalIdentityCode, purposes)) {
      throw new ClientRequestConflictException();
    }
    return FinancingRequestResponseMapper.toCreateResponse(saved, false);
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

  private record ValidatedFinancingRequestInput(
      UUID clientRequestId,
      PersonalIdentityCode personalIdentityCode,
      List<CreditRegisterExtractPurpose> purposes) {}

  private record CompletedPcrCall(
      Instant requestedAt, Instant completedAt, CreditExtract creditExtract) {}
}
