package com.creditlens.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsRequest;
import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsResponse;
import com.creditlens.backend.api.dto.MonitoringFinancingRequestDto;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import com.creditlens.backend.persistence.repository.MonitoringFinancingRequestProjection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MonitoringFinancingRequestServiceTest {
  private static final Instant FROM = Instant.parse("2026-09-18T10:00:00Z");
  private static final Instant TO = Instant.parse("2026-09-18T10:05:00Z");
  private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID EXTRACT_REFERENCE =
      UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Mock FinancingRequestRepository financingRequestRepository;

  @Test
  void mapsProjectionAndPassesIntervalAndPageRequestToRepository() {
    MonitoringFinancingRequestProjection projection = projection();
    when(financingRequestRepository.findMonitoringFinancingRequests(
            FROM, TO, PageRequest.of(2, 25)))
        .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(2, 25), 51));

    ListMonitoringFinancingRequestsResponse response =
        new MonitoringFinancingRequestService(financingRequestRepository)
            .list(new ListMonitoringFinancingRequestsRequest(FROM, TO, 2, 25));

    MonitoringFinancingRequestDto item = response.items().getFirst();
    assertThat(item.financingRequestId()).isEqualTo(REQUEST_ID);
    assertThat(item.extractReference()).isEqualTo(EXTRACT_REFERENCE);
    assertThat(item.maskedPersonalIdentityCode()).isEqualTo("******-123A");
    assertThat(item.voluntaryCreditBanReason().name()).isEqualTo("RiskOfIdentityTheft");
    assertThat(item.lendersCount()).isEqualTo(2);
    assertThat(item.loanContractsCount()).isEqualTo(3);
    assertThat(item.guaranteedLoanContractsCount()).isZero();
    assertThat(response.page()).isEqualTo(2);
    assertThat(response.size()).isEqualTo(25);
    assertThat(response.totalItems()).isEqualTo(51);
    assertThat(response.totalPages()).isEqualTo(3);
    verify(financingRequestRepository)
        .findMonitoringFinancingRequests(FROM, TO, PageRequest.of(2, 25));
  }

  @Test
  void mapsEmptyPageWithoutLoadingOrCallingOtherServices() {
    when(financingRequestRepository.findMonitoringFinancingRequests(
            eq(FROM), eq(TO), eq(PageRequest.of(0, 100))))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

    ListMonitoringFinancingRequestsResponse response =
        new MonitoringFinancingRequestService(financingRequestRepository)
            .list(new ListMonitoringFinancingRequestsRequest(FROM, TO, null, null));

    assertThat(response.items()).isEmpty();
    assertThat(response.page()).isZero();
    assertThat(response.size()).isEqualTo(100);
    assertThat(response.totalItems()).isZero();
    assertThat(response.totalPages()).isZero();
  }

  private MonitoringFinancingRequestProjection projection() {
    return new MonitoringFinancingRequestProjection() {
      @Override
      public UUID getFinancingRequestId() {
        return REQUEST_ID;
      }

      @Override
      public UUID getExtractReference() {
        return EXTRACT_REFERENCE;
      }

      @Override
      public Instant getRequestedAt() {
        return Instant.parse("2026-09-18T10:04:40Z");
      }

      @Override
      public Instant getCompletedAt() {
        return Instant.parse("2026-09-18T10:04:42Z");
      }

      @Override
      public String getPersonalIdentityCode() {
        return "010190-123A";
      }

      @Override
      public String getVoluntaryCreditBanReason() {
        return "RiskOfIdentityTheft";
      }

      @Override
      public int getLendersCount() {
        return 2;
      }

      @Override
      public int getLoanContractsCount() {
        return 3;
      }

      @Override
      public int getGuaranteedLoanContractsCount() {
        return 0;
      }
    };
  }
}
