package com.creditlens.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.support.BackendApiIntegrationTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchFinancingRequestApiIntegrationTest extends BackendApiIntegrationTestSupport {

  @Test
  void searchesOnlyRequestsWithExtractUsingStableDatabasePagination() throws Exception {
    String personalIdentityCode = "010190-123A";
    UUID consumerId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO consumer (id, personal_identity_code, created_at) VALUES (?, ?, ?)",
        consumerId,
        personalIdentityCode,
        java.sql.Timestamp.from(Instant.parse("2026-09-18T10:00:00Z")));
    insertHistoryRow(
        consumerId,
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        UUID.fromString("10000000-0000-0000-0000-000000000001"),
        Instant.parse("2026-09-18T10:15:29Z"),
        UUID.fromString("20000000-0000-0000-0000-000000000001"));
    insertHistoryRow(
        consumerId,
        UUID.fromString("00000000-0000-0000-0000-000000000003"),
        UUID.fromString("10000000-0000-0000-0000-000000000003"),
        Instant.parse("2026-09-18T10:15:30Z"),
        UUID.fromString("20000000-0000-0000-0000-000000000003"));
    insertHistoryRow(
        consumerId,
        UUID.fromString("00000000-0000-0000-0000-000000000002"),
        UUID.fromString("10000000-0000-0000-0000-000000000002"),
        Instant.parse("2026-09-18T10:15:30Z"),
        UUID.fromString("20000000-0000-0000-0000-000000000002"));

    String response =
        mockMvc
            .perform(
                post("/api/v1/financing-requests/search")
                    .contentType("application/json")
                    .content("{\"personalIdentityCode\":\"010190-123A\",\"page\":0,\"size\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value("00000000-0000-0000-0000-000000000003"))
            .andExpect(jsonPath("$.items[1].id").value("00000000-0000-0000-0000-000000000002"))
            .andExpect(jsonPath("$.items[0].maskedPersonalIdentityCode").value("******-123A"))
            .andExpect(
                jsonPath("$.items[0].extractReference")
                    .value("20000000-0000-0000-0000-000000000003"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(personalIdentityCode);
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());

    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType("application/json")
                .content("{\"personalIdentityCode\":\"020290-123A\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalItems").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));

    jdbcTemplate.update(
        "DELETE FROM credit_extract WHERE financing_request_id = ?",
        UUID.fromString("00000000-0000-0000-0000-000000000001"));
    mockMvc
        .perform(
            post("/api/v1/financing-requests/search")
                .contentType("application/json")
                .content("{\"personalIdentityCode\":\"010190-123A\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(2));
  }

  private void insertHistoryRow(
      UUID consumerId,
      UUID requestId,
      UUID clientRequestId,
      Instant requestedAt,
      UUID extractReference) {
    jdbcTemplate.update(
        """
        INSERT INTO financing_request
            (id, consumer_id, client_request_id, extract_purposes, requested_at, completed_at)
        VALUES (?, ?, ?, ARRAY['NewConsumerCredit'], ?, ?)
        """,
        requestId,
        consumerId,
        clientRequestId,
        java.sql.Timestamp.from(requestedAt),
        java.sql.Timestamp.from(requestedAt.plusSeconds(1)));
    jdbcTemplate.update(
        """
        INSERT INTO credit_extract
            (id, financing_request_id, extract_reference, creation_time_utc, voluntary_ban_active,
             voluntary_ban_reason, lenders_count, loan_contracts_count,
             guaranteed_loan_contracts_count, repayment_amounts, leasing_instalment_amounts,
             loans, income_data, persisted_at)
        VALUES (?, ?, ?, ?, FALSE, NULL, 0, 0, 0, '[]', '[]', '[]', '[]', ?)
        """,
        UUID.randomUUID(),
        requestId,
        extractReference,
        java.sql.Timestamp.from(requestedAt.plusSeconds(1)),
        java.sql.Timestamp.from(requestedAt.plusSeconds(1)));
  }
}
