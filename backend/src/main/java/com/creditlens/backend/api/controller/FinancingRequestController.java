package com.creditlens.backend.api.controller;

import com.creditlens.backend.api.dto.CreateFinancingRequestRequest;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.GetFinancingRequestDetailsResponse;
import com.creditlens.backend.api.dto.SearchFinancingRequestsRequest;
import com.creditlens.backend.api.dto.SearchFinancingRequestsResponse;
import com.creditlens.backend.service.FinancingRequestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financing-requests")
public class FinancingRequestController {

  private final FinancingRequestService financingRequestService;

  public FinancingRequestController(FinancingRequestService financingRequestService) {
    this.financingRequestService = financingRequestService;
  }

  @PostMapping
  public ResponseEntity<CreateFinancingRequestResponse> create(
      @Valid @RequestBody CreateFinancingRequestRequest request) {
    CreateFinancingRequestResponse response = financingRequestService.create(request);
    if (!response.newlyCreated()) {
      return ResponseEntity.ok(response);
    }

    URI location = URI.create("/api/v1/financing-requests/" + response.id());
    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/search")
  public ResponseEntity<SearchFinancingRequestsResponse> search(
      @Valid @RequestBody SearchFinancingRequestsRequest request) {
    return ResponseEntity.ok(financingRequestService.searchHistory(request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<GetFinancingRequestDetailsResponse> getDetails(@PathVariable UUID id) {
    return ResponseEntity.ok(financingRequestService.getDetails(id));
  }
}
