package com.creditlens.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.support.BackendApiIntegrationTestSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitoringFinancingRequestApiIntegrationTest extends BackendApiIntegrationTestSupport {

  @Test
  void getsActiveBanCandidatesUsingHalfOpenIntervalAndStableOrder() throws Exception {
    Instant completedFrom = Instant.parse("2026-09-18T10:00:00Z");
    Instant completedTo = Instant.parse("2026-09-18T10:05:00Z");
    UUID atFromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID sameTimeLowerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID sameTimeHigherId = UUID.fromString("00000000-0000-0000-0000-000000000003");

    insertMonitoringRow(
        atFromId,
        completedFrom,
        true,
        "010190-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000001"),
        "ControlOfPersonalFinances");
    insertMonitoringRow(
        sameTimeLowerId,
        Instant.parse("2026-09-18T10:01:00Z"),
        true,
        "020290-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000002"),
        "RiskOfIdentityTheft");
    insertMonitoringRow(
        sameTimeHigherId,
        Instant.parse("2026-09-18T10:01:00Z"),
        true,
        "030390-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000003"),
        "Other");
    insertMonitoringRow(
        UUID.fromString("00000000-0000-0000-0000-000000000004"),
        completedTo,
        true,
        "040490-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000004"),
        "Other");
    insertMonitoringRow(
        UUID.fromString("00000000-0000-0000-0000-000000000005"),
        Instant.parse("2026-09-18T10:06:00Z"),
        true,
        "050590-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000005"),
        "Other");
    insertMonitoringRow(
        UUID.fromString("00000000-0000-0000-0000-000000000006"),
        Instant.parse("2026-09-18T10:02:00Z"),
        false,
        "060690-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000006"),
        null);

    String response =
        mockMvc
            .perform(
                get("/api/v1/monitoring/financing-requests")
                    .param("completedFrom", completedFrom.toString())
                    .param("completedTo", completedTo.toString())
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].financingRequestId").value(atFromId.toString()))
            .andExpect(jsonPath("$.items[1].financingRequestId").value(sameTimeLowerId.toString()))
            .andExpect(jsonPath("$.items[2].financingRequestId").value(sameTimeHigherId.toString()))
            .andExpect(jsonPath("$.items[0].completedAt").value(completedFrom.toString()))
            .andExpect(jsonPath("$.items[0].maskedPersonalIdentityCode").value("******-123A"))
            .andExpect(
                jsonPath("$.items[0].voluntaryCreditBanReason").value("ControlOfPersonalFinances"))
            .andExpect(jsonPath("$.items[0].lendersCount").value(2))
            .andExpect(jsonPath("$.items[0].loanContractsCount").value(3))
            .andExpect(jsonPath("$.items[0].guaranteedLoanContractsCount").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$..personalIdentityCode").isEmpty())
            .andExpect(jsonPath("$..status").isEmpty())
            .andExpect(jsonPath("$..error").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("010190-123A");
    verify(positiveCreditRegisterClient, never()).requestCreditExtract(any(), any());

    mockMvc
        .perform(
            get("/api/v1/monitoring/financing-requests")
                .param("completedFrom", completedFrom.toString())
                .param("completedTo", completedTo.toString())
                .param("page", "1")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].financingRequestId").value(sameTimeHigherId.toString()))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalItems").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  private void insertMonitoringRow(
      UUID requestId,
      Instant completedAt,
      boolean activeBan,
      String personalIdentityCode,
      UUID extractReference,
      String banReason) {
    UUID consumerId = UUID.randomUUID();
    Instant requestedAt = completedAt.minusSeconds(1);
    jdbcTemplate.update(
        "INSERT INTO consumer (id, personal_identity_code, created_at) VALUES (?, ?, ?)",
        consumerId,
        personalIdentityCode,
        java.sql.Timestamp.from(requestedAt));
    jdbcTemplate.update(
        """
        INSERT INTO financing_request
            (id, consumer_id, client_request_id, extract_purposes, requested_at, completed_at)
        VALUES (?, ?, ?, ARRAY['NewConsumerCredit'], ?, ?)
        """,
        requestId,
        consumerId,
        UUID.randomUUID(),
        java.sql.Timestamp.from(requestedAt),
        java.sql.Timestamp.from(completedAt));
    jdbcTemplate.update(
        """
        INSERT INTO credit_extract
            (id, financing_request_id, extract_reference, creation_time_utc, voluntary_ban_active,
             voluntary_ban_reason, lenders_count, loan_contracts_count,
             guaranteed_loan_contracts_count, repayment_amounts, leasing_instalment_amounts,
             loans, income_data, persisted_at)
        VALUES (?, ?, ?, ?, ?, ?, 2, 3, 1, '[]', '[]', '[]', '[]', ?)
        """,
        UUID.randomUUID(),
        requestId,
        extractReference,
        java.sql.Timestamp.from(completedAt),
        activeBan,
        banReason,
        java.sql.Timestamp.from(completedAt));
  }
}
