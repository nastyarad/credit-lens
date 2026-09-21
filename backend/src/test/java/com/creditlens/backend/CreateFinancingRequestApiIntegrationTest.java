package com.creditlens.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.integration.pcr.PositiveCreditRegisterException;
import com.creditlens.backend.support.BackendApiIntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CreateFinancingRequestApiIntegrationTest extends BackendApiIntegrationTestSupport {

  @Test
  void createsRequestAndReadsPersistedResultThroughPublicApi() throws Exception {
    UUID clientRequestId = UUID.randomUUID();
    String personalIdentityCode = "010190-123A";

    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              return invocation.callRealMethod();
            })
        .when(positiveCreditRegisterClient)
        .requestCreditExtract(any(), any());

    String location =
        mockMvc
            .perform(
                post("/api/v1/financing-requests")
                    .contentType("application/json")
                    .content(
                        createRequest(clientRequestId, personalIdentityCode, "NewConsumerCredit")))
            .andExpect(status().isCreated())
            .andExpect(
                header()
                    .string(
                        "Location",
                        org.hamcrest.Matchers.matchesPattern(
                            "/api/v1/financing-requests/[0-9a-f-]+")))
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.clientRequestId").value(clientRequestId.toString()))
            .andExpect(jsonPath("$.consumer.maskedPersonalIdentityCode").value("******-123A"))
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.newlyCreated").doesNotExist())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    String persistedResponse =
        mockMvc
            .perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientRequestId").value(clientRequestId.toString()))
            .andExpect(
                jsonPath("$.creditExtract.extractReference")
                    .value("2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10"))
            .andExpect(jsonPath("$.creditExtract.creditInformationSummary.lendersCount").value(0))
            .andExpect(jsonPath("$.creditExtract.loans").isEmpty())
            .andExpect(jsonPath("$.creditExtract.incomeData").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(persistedResponse).doesNotContain(personalIdentityCode);
    verify(positiveCreditRegisterClient).requestCreditExtract(any(), any());
  }

  @Test
  void repeatsIdempotentlyAndRejectsDifferentPayload() throws Exception {
    UUID clientRequestId = UUID.randomUUID();
    String originalRequest = createRequest(clientRequestId, "010190-123A", "NewConsumerCredit");

    String location =
        mockMvc
            .perform(
                post("/api/v1/financing-requests")
                    .contentType("application/json")
                    .content(originalRequest))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(originalRequest))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(jsonPath("$.id").value(location.substring(location.lastIndexOf('/') + 1)))
        .andExpect(jsonPath("$.creditExtractSummary").isNotEmpty());

    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType("application/json")
                .content("{\"personalIdentityCode\":\"010190-123A\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(1));

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(createRequest(clientRequestId, "010190-123A", "NewLoan")))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.detail").value("clientRequestId was already used with different input"));

    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
  }

  @Test
  void doesNotPersistAnythingWhenPcrTimesOut() throws Exception {
    doThrow(new PositiveCreditRegisterException(PositiveCreditRegisterException.Kind.TIMEOUT))
        .when(positiveCreditRegisterClient)
        .requestCreditExtract(any(), any());

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(createRequest(UUID.randomUUID(), "010190-123A", "NewConsumerCredit")))
        .andExpect(status().isGatewayTimeout())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(504))
        .andExpect(
            jsonPath("$.detail")
                .value("The Positive Credit Register request could not be completed."))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("010190-123A"))));

    // This is an internal atomicity invariant which cannot be proven through a successful-only API.
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class))
        .isZero();
  }
}
