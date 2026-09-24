package com.creditlens.backend.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsRequest;
import com.creditlens.backend.api.dto.ListMonitoringFinancingRequestsResponse;
import com.creditlens.backend.api.dto.MonitoringFinancingRequestDto;
import com.creditlens.backend.api.dto.VoluntaryCreditBanReasonDto;
import com.creditlens.backend.service.MonitoringFinancingRequestService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class MonitoringFinancingRequestControllerTest {
  private static final String FROM = "2026-09-18T10:00:00Z";
  private static final String TO = "2026-09-18T10:05:00Z";

  @Mock MonitoringFinancingRequestService monitoringFinancingRequestService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new MonitoringFinancingRequestController(monitoringFinancingRequestService))
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void returnsMonitoringResponseAndUsesDefaultPagination() throws Exception {
    when(monitoringFinancingRequestService.list(any())).thenReturn(response(List.of(item())));

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedFrom", FROM)
                .param("completedTo", TO))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items[0].financingRequestId").isNotEmpty())
        .andExpect(jsonPath("$.items[0].maskedPersonalIdentityCode").value("******-123A"))
        .andExpect(
            jsonPath("$.items[0].voluntaryCreditBanReason").value("ControlOfPersonalFinances"))
        .andExpect(jsonPath("$.items[0].personalIdentityCode").doesNotExist())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(100))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    ArgumentCaptor<ListMonitoringFinancingRequestsRequest> request =
        ArgumentCaptor.forClass(ListMonitoringFinancingRequestsRequest.class);
    verify(monitoringFinancingRequestService).list(request.capture());
    assertThat(request.getValue().completedFrom()).isEqualTo(Instant.parse(FROM));
    assertThat(request.getValue().completedTo()).isEqualTo(Instant.parse(TO));
    assertThat(request.getValue().page()).isZero();
    assertThat(request.getValue().size()).isEqualTo(100);
  }

  @Test
  void passesExplicitPaginationAndReturnsEmptyItems() throws Exception {
    when(monitoringFinancingRequestService.list(any()))
        .thenReturn(new ListMonitoringFinancingRequestsResponse(List.of(), 2, 25, 0, 0));

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedFrom", FROM)
                .param("completedTo", TO)
                .param("page", "2")
                .param("size", "25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(25))
        .andExpect(jsonPath("$.totalItems").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));

    ArgumentCaptor<ListMonitoringFinancingRequestsRequest> request =
        ArgumentCaptor.forClass(ListMonitoringFinancingRequestsRequest.class);
    verify(monitoringFinancingRequestService).list(request.capture());
    assertThat(request.getValue().page()).isEqualTo(2);
    assertThat(request.getValue().size()).isEqualTo(25);
  }

  @Test
  void returnsProblemDetailsForMissingMalformedAndInvalidParameters() throws Exception {
    String correlationId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedTo", TO)
                .header("X-Correlation-Id", correlationId))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string("X-Correlation-Id", correlationId))
        .andExpect(jsonPath("$.correlationId").value(correlationId))
        .andExpect(jsonPath("$.instance").value("/api/v1/monitoring/financing-requests"));

    mockMvc
        .perform(get("/api/v1/monitoring/financing-requests").param("completedFrom", FROM))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedFrom", "not-an-instant")
                .param("completedTo", TO))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedFrom", TO)
                .param("completedTo", FROM)
                .param("size", "501"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

    verify(monitoringFinancingRequestService, never()).list(any());
  }

  private ListMonitoringFinancingRequestsResponse response(
      List<MonitoringFinancingRequestDto> items) {
    return new ListMonitoringFinancingRequestsResponse(items, 0, 100, items.size(), 1);
  }

  private MonitoringFinancingRequestDto item() {
    return new MonitoringFinancingRequestDto(
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        Instant.parse("2026-09-18T10:04:40Z"),
        Instant.parse("2026-09-18T10:04:42Z"),
        "******-123A",
        VoluntaryCreditBanReasonDto.ControlOfPersonalFinances,
        2,
        3,
        0);
  }
}
