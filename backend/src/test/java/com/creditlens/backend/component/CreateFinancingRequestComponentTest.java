package com.creditlens.backend.component;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditlens.backend.api.controller.FinancingRequestController;
import com.creditlens.backend.api.dto.CreateFinancingRequestRequest;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.domain.Consumer;
import com.creditlens.backend.domain.CreditExtract;
import com.creditlens.backend.domain.CreditInformationSummary;
import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.FinancingRequest;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.domain.VoluntaryBanOnCredits;
import com.creditlens.backend.integration.pcr.HttpPositiveCreditRegisterClient;
import com.creditlens.backend.integration.pcr.PcrProperties;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterException;
import com.creditlens.backend.persistence.entity.ConsumerEntity;
import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import com.creditlens.backend.persistence.entity.FinancingRequestEntity;
import com.creditlens.backend.persistence.repository.ConsumerRepository;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import com.creditlens.backend.service.FinancingRequestService;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class CreateFinancingRequestComponentTest {
  private static final UUID CLIENT_REQUEST_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID CONSUMER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final Instant NOW = Instant.parse("2026-09-19T10:15:30Z");

  @RegisterExtension
  static final WireMockExtension wireMock =
      WireMockExtension.newInstance()
          .options(options().dynamicPort().usingFilesUnderClasspath("wiremock"))
          .build();

  @Mock ConsumerRepository consumerRepository;
  @Mock FinancingRequestRepository financingRequestRepository;
  @Mock CreditExtractRepository creditExtractRepository;
  @Mock PlatformTransactionManager transactionManager;
  @Mock TransactionStatus transactionStatus;

  private FinancingRequestController controller;

  @BeforeEach
  void setUp() {
    var properties =
        new PcrProperties(
            wireMock.baseUrl(),
            "Test",
            new PcrProperties.Owner("BusinessId", "1234567-8", "FI"),
            Duration.ofSeconds(1),
            Duration.ofSeconds(1));
    var pcrClient = new HttpPositiveCreditRegisterClient(RestClient.builder(), properties);
    var service =
        new FinancingRequestService(
            consumerRepository,
            financingRequestRepository,
            creditExtractRepository,
            pcrClient,
            transactionManager,
            Clock.fixed(NOW, ZoneOffset.UTC));
    controller = new FinancingRequestController(service);
    lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
  }

  @Test
  void createsRequestUsingRealPcrHttpAdapterAndPassesAggregateToRepositories() {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withBodyFile("pcr-complete-extract.json")));
    ConsumerEntity consumer = consumerEntity();
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.empty());
    when(consumerRepository.findByPersonalIdentityCode("010190-123A"))
        .thenReturn(Optional.of(consumer));
    when(financingRequestRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(creditExtractRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = controller.create(request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().consumer().maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(response.getBody().creditExtractSummary().extractReference().toString())
        .isEqualTo("55555555-5555-5555-5555-555555555555");
    ArgumentCaptor<FinancingRequestEntity> requestCaptor =
        ArgumentCaptor.forClass(FinancingRequestEntity.class);
    verify(financingRequestRepository).save(requestCaptor.capture());
    assertThat(requestCaptor.getValue().getClientRequestId()).isEqualTo(CLIENT_REQUEST_ID);
    assertThat(requestCaptor.getValue().getConsumer().getId()).isEqualTo(CONSUMER_ID);
    verify(creditExtractRepository).save(any(CreditExtractEntity.class));
    wireMock.verify(1, postRequestedFor(urlEqualTo("/GetCreditRegisterExtract")));
  }

  @Test
  void doesNotSaveWhenPcrCallFails() {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract")).willReturn(aResponse().withStatus(503)));
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> controller.create(request()))
        .isInstanceOfSatisfying(
            PositiveCreditRegisterException.class,
            exception ->
                assertThat(exception.kind())
                    .isEqualTo(PositiveCreditRegisterException.Kind.UNAVAILABLE));

    verify(consumerRepository, never()).save(any());
    verify(financingRequestRepository, never()).save(any());
    verify(creditExtractRepository, never()).save(any());
    wireMock.verify(1, postRequestedFor(urlEqualTo("/GetCreditRegisterExtract")));
  }

  @Test
  void returnsExistingRequestWithoutCallingPcrOrSaving() {
    FinancingRequestEntity existing = existingEntity();
    when(financingRequestRepository.findByClientRequestId(CLIENT_REQUEST_ID))
        .thenReturn(Optional.of(existing));
    when(creditExtractRepository.findByFinancingRequest_Id(REQUEST_ID))
        .thenReturn(Optional.of(new CreditExtractEntity(emptyExtract(), existing)));

    var response = controller.create(request());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(REQUEST_ID);
    verify(financingRequestRepository, never()).save(any());
    verify(creditExtractRepository, never()).save(any());
    wireMock.verify(0, postRequestedFor(urlEqualTo("/GetCreditRegisterExtract")));
  }

  private CreateFinancingRequestRequest request() {
    return new CreateFinancingRequestRequest(
        CLIENT_REQUEST_ID,
        "010190-123A",
        List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit));
  }

  private ConsumerEntity consumerEntity() {
    return new ConsumerEntity(
        new Consumer(CONSUMER_ID, PersonalIdentityCode.of("010190-123A"), NOW.minusSeconds(1)));
  }

  private FinancingRequestEntity existingEntity() {
    ConsumerEntity consumer = consumerEntity();
    return new FinancingRequestEntity(
        FinancingRequest.create(
            REQUEST_ID,
            CLIENT_REQUEST_ID,
            consumer.toDomain(),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit),
            NOW.minusSeconds(1),
            NOW,
            emptyExtract()),
        consumer);
  }

  private CreditExtract emptyExtract() {
    return new CreditExtract(
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        NOW,
        new VoluntaryBanOnCredits(false, null),
        new CreditInformationSummary(0, 0, 0),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        NOW);
  }
}
