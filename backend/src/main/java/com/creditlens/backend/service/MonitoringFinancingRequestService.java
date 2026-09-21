package com.creditlens.backend.service;

import com.creditlens.backend.api.dto.GetMonitoringFinancingRequestsRequest;
import com.creditlens.backend.api.dto.GetMonitoringFinancingRequestsResponse;
import com.creditlens.backend.api.dto.MonitoringFinancingRequestDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.creditlens.backend.persistence.repository.FinancingRequestRepository;
import com.creditlens.backend.persistence.repository.MonitoringFinancingRequestProjection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class MonitoringFinancingRequestService {

  private final FinancingRequestRepository financingRequestRepository;

  public MonitoringFinancingRequestService(FinancingRequestRepository financingRequestRepository) {
    this.financingRequestRepository = financingRequestRepository;
  }

  public GetMonitoringFinancingRequestsResponse get(GetMonitoringFinancingRequestsRequest request) {
    Page<MonitoringFinancingRequestProjection> results =
        financingRequestRepository.findMonitoringFinancingRequests(
            request.completedFrom(),
            request.completedTo(),
            PageRequest.of(request.page(), request.size()));
    List<MonitoringFinancingRequestDto> items =
        results.getContent().stream().map(this::toDto).toList();
    return new GetMonitoringFinancingRequestsResponse(
        items,
        results.getNumber(),
        results.getSize(),
        results.getTotalElements(),
        results.getTotalPages());
  }

  private MonitoringFinancingRequestDto toDto(MonitoringFinancingRequestProjection item) {
    return new MonitoringFinancingRequestDto(
        item.getFinancingRequestId(),
        item.getExtractReference(),
        item.getRequestedAt(),
        item.getCompletedAt(),
        PersonalIdentityCode.of(item.getPersonalIdentityCode()).masked(),
        VoluntaryCreditBanReasonDto.valueOf(item.getVoluntaryCreditBanReason()),
        item.getLendersCount(),
        item.getLoanContractsCount(),
        item.getGuaranteedLoanContractsCount());
  }
}
