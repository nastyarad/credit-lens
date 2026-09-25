package com.creditlens.backend.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.api.dto.ConsumerDto;
import com.creditlens.backend.api.dto.CreateFinancingRequestResponse;
import com.creditlens.backend.api.dto.CreditExtractSummaryDto;
import com.creditlens.backend.api.dto.CreditRegisterExtractPurposeDto;
import com.creditlens.backend.api.dto.FinancingRequestHistoryItemDto;
import com.creditlens.backend.api.dto.SearchFinancingRequestsResponse;
import com.creditlens.backend.api.dto.VoluntaryBanOnCreditsDto;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterException;
import com.creditlens.backend.service.ClientRequestConflictException;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class FinancingRequestControllerTest {
  private static final UUID REQUEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID CLIENT_REQUEST_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock FinancingRequestService financingRequestService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FinancingRequestController(financingRequestService))
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void returnsCreatedAndLocationForNewRequest() throws Exception {
    when(financingRequestService.create(any())).thenReturn(response(true));

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/financing-requests/" + REQUEST_ID))
        .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()))
        .andExpect(jsonPath("$.newlyCreated").doesNotExist());

    verify(financingRequestService).create(any());
  }

  @Test
  void returnsOkWithoutLocationForIdempotentRequest() throws Exception {
    when(financingRequestService.create(any())).thenReturn(response(false));

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(jsonPath("$.id").value(REQUEST_ID.toString()));
  }

  @Test
  void rejectsInvalidRequestBeforeCallingService() throws Exception {
    String correlationId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"clientRequestId":"%s","personalIdentityCode":"invalid","creditRegisterExtractPurposes":[]}
                                """
                        .formatted(CLIENT_REQUEST_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(header().string("X-Correlation-Id", correlationId))
        .andExpect(jsonPath("$.correlationId").value(correlationId))
        .andExpect(jsonPath("$.detail").value("The request could not be validated."))
        .andExpect(jsonPath("$..personalIdentityCode").isEmpty());
  }

  @Test
  void mapsClientRequestConflictToProblemDetails() throws Exception {
    when(financingRequestService.create(any())).thenThrow(new ClientRequestConflictException());
    String correlationId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isConflict())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(header().string("X-Correlation-Id", correlationId))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Client request ID conflict"))
        .andExpect(jsonPath("$.instance").value("/api/v1/financing-requests"));
  }

  @Test
  void includesCorrelationIdInPcrProblemResponseAndHeader() throws Exception {
    String correlationId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    when(financingRequestService.create(any()))
        .thenThrow(
            new PositiveCreditRegisterException(PositiveCreditRegisterException.Kind.TIMEOUT));

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .header("X-Correlation-Id", correlationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
        .andExpect(status().isGatewayTimeout())
        .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        .andExpect(header().string("X-Correlation-Id", correlationId))
        .andExpect(jsonPath("$.correlationId").value(correlationId))
        .andExpect(
            jsonPath("$.detail")
                .value("The Positive Credit Register request could not be completed."));
  }

  @Test
  void searchesHistoryWithValidatedRequestAndPageMetadata() throws Exception {
    when(financingRequestService.searchHistory(any()))
        .thenReturn(
            new SearchFinancingRequestsResponse(
                List.of(
                    new FinancingRequestHistoryItemDto(
                        REQUEST_ID,
                        CLIENT_REQUEST_ID,
                        "******-123A",
                        Instant.parse("2026-09-19T10:15:29Z"),
                        Instant.parse("2026-09-19T10:15:30Z"),
                        UUID.fromString("55555555-5555-5555-5555-555555555555"),
                        true)),
                0,
                20,
                1,
                1));

    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personalIdentityCode\":\"010190-123A\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].maskedPersonalIdentityCode").value("******-123A"))
        .andExpect(jsonPath("$.items[0].voluntaryCreditBanActive").value(true))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$..personalIdentityCode").isEmpty());

    verify(financingRequestService).searchHistory(any());
  }

  @Test
  void rejectsInvalidHistorySearchBeforeCallingService() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personalIdentityCode\":\"010190-123A\",\"page\":-1}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personalIdentityCode\":\"invalid\",\"size\":101}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personalIdentityCode\":\"010190-123A\",\"size\":0}"))
        .andExpect(status().isBadRequest());

    verify(financingRequestService, org.mockito.Mockito.never()).searchHistory(any());
  }

  private String validRequestJson() {
    return """
                {"clientRequestId":"%s","personalIdentityCode":"010190-123A","creditRegisterExtractPurposes":["NewConsumerCredit"]}
                """
        .formatted(CLIENT_REQUEST_ID);
  }

  private CreateFinancingRequestResponse response(boolean newlyCreated) {
    return new CreateFinancingRequestResponse(
        REQUEST_ID,
        CLIENT_REQUEST_ID,
        new ConsumerDto(UUID.fromString("33333333-3333-3333-3333-333333333333"), "******-123A"),
        List.of(CreditRegisterExtractPurposeDto.NewConsumerCredit),
        Instant.parse("2026-09-19T10:15:29Z"),
        Instant.parse("2026-09-19T10:15:30Z"),
        new CreditExtractSummaryDto(
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
            Instant.parse("2026-09-19T10:15:30Z"),
            new VoluntaryBanOnCreditsDto(false, null),
            0,
            0,
            0),
        newlyCreated);
  }
}
