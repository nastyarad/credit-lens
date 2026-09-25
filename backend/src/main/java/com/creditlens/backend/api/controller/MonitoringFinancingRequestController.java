package com.creditlens.backend.api.controller;

import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsRequest;
import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsResponse;
import com.creditlens.backend.service.MonitoringFinancingRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/financing-requests")
public class MonitoringFinancingRequestController {

  private final MonitoringFinancingRequestService monitoringFinancingRequestService;

  public MonitoringFinancingRequestController(
      MonitoringFinancingRequestService monitoringFinancingRequestService) {
    this.monitoringFinancingRequestService = monitoringFinancingRequestService;
  }

  @GetMapping
  public ResponseEntity<ListMonitoringFinancingRequestsResponse> list(
      @Valid @ModelAttribute ListMonitoringFinancingRequestsRequest request) {
    return ResponseEntity.ok(monitoringFinancingRequestService.list(request));
  }
}
