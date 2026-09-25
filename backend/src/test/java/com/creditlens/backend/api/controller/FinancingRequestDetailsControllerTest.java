package com.creditlens.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreditExtractDto;
import com.creditlens.backend.api.dto.CreditInformationSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.GetFinancingRequestDetailsResponse;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.service.FinancingRequestNotFoundException;
import com.creditlens.backend.service.FinancingRequestService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FinancingRequestDetailsControllerTest {
  private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock FinancingRequestService financingRequestService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FinancingRequestController(financingRequestService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  void returnsCompleteDetailsWithoutSensitiveOrLifecycleFields() throws Exception {
    when(financingRequestService.getDetails(REQUEST_ID)).thenReturn(detailsResponse());

    mockMvc
        .perform(get("/api/v1/financing-requests/{id}", REQUEST_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
        .andExpect(jsonPath("$.consumer.maskedPersonalIdentityCode").value("******-123A"))
        .andExpect(jsonPath("$.creditExtract.creditInformationSummary.lendersCount").value(2))
        .andExpect(jsonPath("$.creditExtract.loans").isArray())
        .andExpect(jsonPath("$.creditExtract.incomeData").isArray())
        .andExpect(jsonPath("$..personalIdentityCode").isEmpty())
        .andExpect(jsonPath("$..status").isEmpty())
        .andExpect(jsonPath("$..error").isEmpty())
        .andExpect(jsonPath("$..newlyCreated").isEmpty());

    verify(financingRequestService).getDetails(REQUEST_ID);
  }

  @Test
  void returnsSafeNotFoundProblemWithCorrelationId() throws Exception {
    when(financingRequestService.getDetails(REQUEST_ID))
        .thenThrow(new FinancingRequestNotFoundException(REQUEST_ID));
    String correlationId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    mockMvc
        .perform(
            get("/api/v1/financing-requests/{id}", REQUEST_ID)
                .header("X-Correlation-Id", correlationId))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(header().string("X-Correlation-Id", correlationId))
        .andExpect(jsonPath("$.title").value("Financing request not found"))
        .andExpect(jsonPath("$.detail").value("The requested financing request was not found."))
        .andExpect(jsonPath("$.instance").value("/api/v1/financing-requests/" + REQUEST_ID))
        .andExpect(jsonPath("$.correlationId").value(correlationId));
  }

  @Test
  void returnsSafeBadRequestForMalformedUuidWithoutCallingService() throws Exception {
    mockMvc
        .perform(get("/api/v1/financing-requests/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE));

    verify(financingRequestService, never()).getDetails(any());
  }

  private GetFinancingRequestDetailsResponse detailsResponse() {
    return new GetFinancingRequestDetailsResponse(
        REQUEST_ID,
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        new ConsumerDto(UUID.fromString("33333333-3333-3333-3333-333333333333"), "******-123A"),
        List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
        Instant.parse("2026-09-19T10:15:29Z"),
        Instant.parse("2026-09-19T10:15:30Z"),
        new CreditExtractDto(
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
            Instant.parse("2026-09-19T10:15:30Z"),
            new VoluntaryBanOnCreditsDto(false, null),
            new CreditInformationSummaryDto(2, 3, 0, List.of(), List.of()),
            List.of(),
            List.of()));
  }
}
