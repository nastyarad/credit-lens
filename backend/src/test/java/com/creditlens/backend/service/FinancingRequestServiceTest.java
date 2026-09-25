package com.creditlens.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditlens.backend.api.dto.CreateFinancingRequestRequest;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.GetFinancingRequestDetailsResponse;
import com.creditlens.backend.api.dto.SearchFinancingRequestsRequest;
import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class FinancingRequestServiceTest {
  private static final UUID CLIENT_REQUEST_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID CONSUMER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID EXTRACT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID EXTRACT_REFERENCE =
      UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final Instant REQUESTED_AT = Instant.parse("2026-09-19T10:15:29Z");
  private static final Instant COMPLETED_AT = Instant.parse("2026-09-19T10:15:30Z");
  private static final String PERSONAL_IDENTITY_CODE = "010190-123A";

  @Mock ConsumerRepository consumerRepository;
  @Mock FinancingRequestRepository financingRequestRepository;
  @Mock CreditExtractRepository creditExtractRepository;
  @Mock PositiveCreditRegisterClient positiveCreditRegisterClient;
  @Mock PlatformTransactionManager transactionManager;
  @Mock TransactionStatus transactionStatus;

  private FinancingRequestService service;

  @BeforeEach
  void setUp() {
    service =
        new FinancingRequestService(
            consumerRepository,
            financingRequestRepository,
            creditExtractRepository,
            positiveCreditRegisterClient,
            transactionManager,
            Clock.fixed(REQUESTED_AT, ZoneOffset.UTC));
    lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
  }

  @Test
  void createsAndPersistsCompletedRequestAfterOnePcrCall() {
    CreateFinancingRequestRequest request =
        request(CreditRegisterExtractPurposeDto.NewConsumerCredit);
    ConsumerEntity consumer =
        new ConsumerEntity(
            new Consumer(
                CONSUMER_ID, PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE), REQUESTED_AT));
    CreditExtract pcrExtract = emptyExtract();

    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.empty());
    when(positiveCreditRegisterClient.requestCreditExtract(any(), any())).thenReturn(pcrExtract);
    when(consumerRepository.findByPersonalIdentityCode(PERSONAL_IDENTITY_CODE))
        .thenReturn(Optional.of(consumer));
    when(financingRequestRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(creditExtractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    CreateFinancingRequestResponse response = service.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.clientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
    assertThat(response.consumer().id()).isEqualTo(CONSUMER_ID);
    assertThat(response.consumer().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(response.creditExtractSummary().extractReference()).isEqualTo(EXTRACT_REFERENCE);
    assertThat(response.creditExtractSummary().lendersCount()).isZero();
    assertThat(response.creditExtractSummary().loanContractsCount()).isZero();
    assertThat(response.creditExtractSummary().guaranteedLoanContractsCount()).isZero();
    assertThat(response.creditExtractSummary().voluntaryBanOnCredits().isInEffect()).isFalse();
    assertThat(response.creditExtractSummary().voluntaryBanOnCredits().reason()).isNull();
    assertThat(response.newlyCreated()).isTrue();

    InOrder order =
        inOrder(
            financingRequestRepository,
            positiveCreditRegisterClient,
            consumerRepository,
            creditExtractRepository);
    order.verify(financingRequestRepository).findByClientRequestId(CLIENT_REQUEST_ID);
    order
        .verify(positiveCreditRegisterClient)
        .requestCreditExtract(
            PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    order.verify(consumerRepository).findByPersonalIdentityCode(PERSONAL_IDENTITY_CODE);
    order.verify(financingRequestRepository).save(any(FinancingRequestEntity.class));
    order.verify(creditExtractRepository).save(any(CreditExtractEntity.class));
    verify(positiveCreditRegisterClient).requestCreditExtract(any(), any());
  }

  @Test
  void passesNormalizedIdentityCodeAndSelectedPurposesToPcr() {
    CreateFinancingRequestRequest request =
        request(
            CreditRegisterExtractPurposeDto.NewConsumerCredit,
            CreditRegisterExtractPurposeDto.NewLoan);
    stubNewRequest();

    service.create(request);

    verify(positiveCreditRegisterClient)
        .requestCreditExtract(
            PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE),
            List.of(
                CreditRegisterExtractPurpose.NewConsumerCredit,
                CreditRegisterExtractPurpose.NewLoan));
  }

  @Test
  void returnsExistingCompletedRequestWithoutCallingPcrOrSaving() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    CreateFinancingRequestResponse response =
        service.create(request(CreditRegisterExtractPurposeDto.NewConsumerCredit));

    assertThat(response.id()).isEqualTo(REQUEST_ID);
    assertThat(response.requestedAt()).isEqualTo(REQUESTED_AT);
    assertThat(response.completedAt()).isEqualTo(COMPLETED_AT);
    assertThat(response.newlyCreated()).isFalse();
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());
    verify(financingRequestRepository, never()).save(any());
    verify(creditExtractRepository, never()).save(any());
  }

  @Test
  void rejectsSameClientRequestIdWithDifferentConsumer() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    Throwable thrown =
        org.assertj.core.api.Assertions.catchThrowable(
            () ->
                service.create(
                    new CreateFinancingRequestRequest(
                        CLIENT_REQUEST_ID,
                        "020290-123A",
                        List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit))));
    assertThat(thrown)
        .isInstanceOf(ClientRequestConflictException.class)
        .hasMessage("clientRequestId was already used with different input");
    assertThat(thrown.getMessage()).doesNotContain("020290-123A");
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());
    verify(financingRequestRepository, never()).save(any());
  }

  @Test
  void rejectsSameClientRequestIdWithDifferentPurposes() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    assertThatThrownBy(() -> service.create(request(CreditRegisterExtractPurposeDto.NewLoan)))
        .isInstanceOf(ClientRequestConflictException.class);
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());
    verify(financingRequestRepository, never()).save(any());
  }

  @Test
  void returnsRequestPersistedByConcurrentCallerAfterClientRequestIdConflict() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    ConsumerEntity consumer =
        new ConsumerEntity(
            new Consumer(
                CONSUMER_ID, PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE), REQUESTED_AT));
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existing));
    when(positiveCreditRegisterClient.requestCreditExtract(any(), any()))
        .thenReturn(emptyExtract());
    when(consumerRepository.findByPersonalIdentityCode(PERSONAL_IDENTITY_CODE))
        .thenReturn(Optional.of(consumer));
    when(financingRequestRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate client request ID"));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    CreateFinancingRequestResponse response =
        service.create(request(CreditRegisterExtractPurposeDto.NewConsumerCredit));

    assertThat(response.id()).isEqualTo(REQUEST_ID);
    assertThat(response.newlyCreated()).isFalse();
    verify(positiveCreditRegisterClient).requestCreditExtract(any(), any());
  }

  @Test
  void searchesHistoryWithDatabasePaginationAndMasksIdentityCodeWithoutCallingPcr() {
    FinancingRequestHistoryProjection item =
        new FinancingRequestHistoryProjection() {
          public UUID getId() {
            return REQUEST_ID;
          }

          public UUID getClientRequestId() {
            return CLIENT_REQUEST_ID;
          }

          public String getPersonalIdentityCode() {
            return PERSONAL_IDENTITY_CODE;
          }

          public Instant getRequestedAt() {
            return REQUESTED_AT;
          }

          public Instant getCompletedAt() {
            return COMPLETED_AT;
          }

          public UUID getExtractReference() {
            return EXTRACT_REFERENCE;
          }

          public boolean isVoluntaryCreditBanActive() {
            return true;
          }
        };
    when(financingRequestRepository.findHistoryByPersonalIdentityCode(any(), any()))
        .thenReturn(
            new PageImpl<>(List.of(item), org.springframework.data.domain.PageRequest.of(1, 2), 3));

    var result =
        service.searchHistory(new SearchFinancingRequestsRequest(PERSONAL_IDENTITY_CODE, 1, 2));

    assertThat(result.items()).hasSize(1);
    assertThat(result.items().getFirst().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(result.items().getFirst().voluntaryCreditBanActive()).isTrue();
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(result.totalPages()).isEqualTo(2);
    verify(financingRequestRepository)
        .findHistoryByPersonalIdentityCode(
            PERSONAL_IDENTITY_CODE, org.springframework.data.domain.PageRequest.of(1, 2));
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());
  }

  @Test
  void loadsAndMapsSavedRequestDetailsWithoutCallingPcr() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    when(financingRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    GetFinancingRequestDetailsResponse result = service.getDetails(REQUEST_ID);

    assertThat(result.id()).isEqualTo(REQUEST_ID);
    assertThat(result.consumer().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(result.creditExtract().extractReference()).isEqualTo(EXTRACT_REFERENCE);
    assertThat(result.creditExtract().loans()).isEmpty();
    assertThat(result.creditExtract().incomeData()).isEmpty();
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());
  }

  @Test
  void returnsNotFoundWhenSavedRequestDoesNotExist() {
    when(financingRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDetails(REQUEST_ID))
        .isInstanceOf(FinancingRequestNotFoundException.class)
        .hasMessage("The requested financing request was not found.");
    verify(creditExtractRepository, never()).findByFinancingRequest_Id(any());
  }

  @Test
  void rejectsSavedRequestWithoutCreditExtractAsInvariantViolation() {
    FinancingRequestEntity existing =
        existingEntity(List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
    when(financingRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDetails(REQUEST_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("saved financing request has no credit extract");
  }

  private void stubNewRequest() {
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.empty());
    when(positiveCreditRegisterClient.requestCreditExtract(any(), any()))
        .thenReturn(emptyExtract());
    when(consumerRepository.findByPersonalIdentityCode(PERSONAL_IDENTITY_CODE))
        .thenReturn(
            Optional.of(
                new ConsumerEntity(
                    new Consumer(
                        CONSUMER_ID,
                        PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE),
                        REQUESTED_AT))));
    when(financingRequestRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(creditExtractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private FinancingRequestEntity existingEntity(List<CreditRegisterExtractPurpose> purposes) {
    ConsumerEntity consumer =
        new ConsumerEntity(
            new Consumer(
                CONSUMER_ID, PersonalIdentityCode.of(PERSONAL_IDENTITY_CODE), REQUESTED_AT));
    return new FinancingRequestEntity(
        FinancingRequest.create(
            REQUEST_ID,
            CLIENT_REQUEST_ID,
            consumer.toDomain(),
            purposes,
            REQUESTED_AT,
            COMPLETED_AT,
            emptyExtract()),
        consumer);
  }

  private CreateFinancingRequestRequest request(CreditRegisterExtractPurposeDto... purposes) {
    return new CreateFinancingRequestRequest(
        CLIENT_REQUEST_ID, PERSONAL_IDENTITY_CODE, List.of(purposes));
  }

  private CreditExtract emptyExtract() {
    return new CreditExtract(
        EXTRACT_ID,
        EXTRACT_REFERENCE,
        COMPLETED_AT,
        new VoluntaryBanOnCredits(false, null),
        new CreditInformationSummary(0, 0, 0),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        COMPLETED_AT);
  }
}
