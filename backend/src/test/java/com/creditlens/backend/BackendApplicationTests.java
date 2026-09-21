package com.creditlens.backend;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.creditlens.backend.integration.pcr.PositiveCreditRegisterClient;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterException;
import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class BackendApplicationTests {

  @RegisterExtension
  static final WireMockExtension wireMock =
      WireMockExtension.newInstance().options(options().dynamicPort()).build();

  @DynamicPropertySource
  static void pcrProperties(DynamicPropertyRegistry registry) {
    registry.add("pcr.base-url", wireMock::baseUrl);
  }

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired JdbcTemplate jdbcTemplate;

  @Autowired MockMvc mockMvc;

  @Autowired CreditExtractRepository creditExtractRepository;

  @MockitoSpyBean PositiveCreditRegisterClient positiveCreditRegisterClient;

  @BeforeEach
  void cleanBackendTables() {
    jdbcTemplate.update("DELETE FROM credit_extract");
    jdbcTemplate.update("DELETE FROM financing_request");
    jdbcTemplate.update("DELETE FROM consumer");
    clearInvocations(positiveCreditRegisterClient);
    wireMock.resetAll();
    wireMock.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                okJson(
                    """
                {"creditRegisterExtract":{"extractReference":"2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10","creationTimeUtc":"2026-09-18T10:15:30Z","personRequested":{"idCode":"010190-123A"},"creditInformationSummary":{"lendersCount":0,"loanContractsCount":0,"guaranteedLoanContractsCount":0},"repaymentsPaidLastAmount":[],"sumOfMonthlyLeasingInstalments":[],"loans":[],"incomeData":[]}}
                """)));
  }

  @Test
  void appliesDatabaseMigration() {
    Integer appliedMigrationCount =
        jdbcTemplate.queryForObject(
            """
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '1' AND success = TRUE
				""",
            Integer.class);

    assertThat(appliedMigrationCount).isEqualTo(1);
  }

  @Test
  void createsCompletedRequestAndPersistsSchemaCompatibleData() throws Exception {
    UUID clientRequestId = UUID.randomUUID();
    String personalIdentityCode = "010190-123A";

    doAnswer(
            invocation -> {
              assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
              return invocation.callRealMethod();
            })
        .when(positiveCreditRegisterClient)
        .requestCreditExtract(any(), any());

    String response =
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
            .andExpect(jsonPath("$.requestedAt").isNotEmpty())
            .andExpect(jsonPath("$.completedAt").isNotEmpty())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.newlyCreated").doesNotExist())
            .andExpect(jsonPath("$.creditExtractSummary.extractReference").isNotEmpty())
            .andExpect(jsonPath("$.creditExtractSummary.lendersCount").value(0))
            .andExpect(jsonPath("$.creditExtractSummary.loanContractsCount").value(0))
            .andExpect(jsonPath("$.creditExtractSummary.guaranteedLoanContractsCount").value(0))
            .andExpect(
                jsonPath("$.creditExtractSummary.voluntaryBanOnCredits.isInEffect").value(false))
            .andExpect(
                jsonPath("$.creditExtractSummary.voluntaryBanOnCredits.reason").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain(personalIdentityCode);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class))
        .isEqualTo(1);

    Map<String, Object> requestRow =
        jdbcTemplate.queryForMap(
            """
				SELECT requested_at, completed_at
				FROM financing_request
				WHERE client_request_id = ?
				""",
            clientRequestId);
    Instant requestedAt = ((java.sql.Timestamp) requestRow.get("requested_at")).toInstant();
    Instant completedAt = ((java.sql.Timestamp) requestRow.get("completed_at")).toInstant();
    assertThat(completedAt).isAfterOrEqualTo(requestedAt);

    Map<String, Object> jsonFields =
        jdbcTemplate.queryForMap(
            """
				SELECT repayment_amounts::text AS repayments,
				       leasing_instalment_amounts::text AS leasing,
				       loans::text AS loans,
				       income_data::text AS income
				FROM credit_extract
				""");
    assertThat(jsonFields)
        .containsEntry("repayments", "[]")
        .containsEntry("leasing", "[]")
        .containsEntry("loans", "[]")
        .containsEntry("income", "[]");

    UUID financingRequestId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM financing_request WHERE client_request_id = ?",
            UUID.class,
            clientRequestId);
    CreditExtractEntity reloadedExtract =
        creditExtractRepository.findByFinancingRequest_Id(financingRequestId).orElseThrow();
    assertThat(reloadedExtract.getRepaymentAmounts()).isEmpty();
    assertThat(reloadedExtract.getLeasingInstalmentAmounts()).isEmpty();
    assertThat(reloadedExtract.getLoans()).isEmpty();
    assertThat(reloadedExtract.getIncomeData()).isEmpty();
    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
  }

  @Test
  void getsPersistedRequestDetailsWithCompleteExtractAndMaskedIdentityCode() throws Exception {
    wireMock.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                okJson(
                    """
                    {"creditRegisterExtract":{"extractReference":"55555555-5555-5555-5555-555555555555","creationTimeUtc":"2026-09-18T10:15:30Z","personRequested":{"idCode":"010190-123A"},"creditInformationSummary":{"lendersCount":2,"loanContractsCount":1,"guaranteedLoanContractsCount":0},"repaymentsPaidLastAmount":[{"currencyCode":"EUR","sum":125.50}],"sumOfMonthlyLeasingInstalments":[{"currencyCode":"EUR","sum":250.00}],"loans":[{"loanType":"LumpSumLoan","contractDate":"2025-01-15T00:00:00Z","isLoanWithCollateral":true,"collateralTypes":["ApartmentOrRealEstate"],"borrowersCount":1,"currencyCode":"EUR","paymentPlan":{"isInDebtArrangement":false,"isInBusinessRestructuringProgram":false},"accuracyIsDenied":false,"lumpSumLoan":{"amountIssued":10000,"amountPaid":1500,"balance":8500,"plannedFinalDueDate":"2030-01-15T00:00:00Z","amortizationFrequency":12},"delayedAmounts":[{"delayedInstalment":100,"originalDueDate":"2026-08-15T00:00:00Z"}],"isForeclosed":false}],"incomeData":[{"year":2026,"months":[{"month":8,"wagesGrossAmount":4000,"wagesNetAmount":3000,"benefitsGrossAmount":0,"benefitsNetAmount":0}]}]}}
                    """)));

    String location =
        mockMvc
            .perform(
                post("/api/v1/financing-requests")
                    .contentType("application/json")
                    .content(createRequest(UUID.randomUUID(), "010190-123A", "NewConsumerCredit")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    String response =
        mockMvc
            .perform(get(location))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.consumer.maskedPersonalIdentityCode").value("******-123A"))
            .andExpect(
                jsonPath("$.creditExtract.extractReference")
                    .value("55555555-5555-5555-5555-555555555555"))
            .andExpect(
                jsonPath(
                        "$.creditExtract.creditInformationSummary.repaymentsPaidLastAmount[0].currencyCode")
                    .value("EUR"))
            .andExpect(
                jsonPath(
                        "$.creditExtract.creditInformationSummary.sumOfMonthlyLeasingInstalments[0].sum")
                    .value(250.0))
            .andExpect(jsonPath("$.creditExtract.loans[0].loanType").value("LumpSumLoan"))
            .andExpect(jsonPath("$.creditExtract.loans[0].lumpSumLoan.balance").value(8500))
            .andExpect(
                jsonPath("$.creditExtract.loans[0].delayedAmount[0].isForeclosed").value(false))
            .andExpect(
                jsonPath("$.creditExtract.incomeData[0].months[0].wagesNetAmount").value(3000))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(response).doesNotContain("010190-123A");
    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
  }

  @Test
  void searchesOnlyCompletedHistoryWithExtractAndDoesNotCallPcr() throws Exception {
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

  @Test
  void getsMonitoringCandidatesUsingHalfOpenIntervalAndStableOrder() throws Exception {
    Instant completedFrom = Instant.parse("2026-09-18T10:00:00Z");
    Instant completedTo = Instant.parse("2026-09-18T10:05:00Z");
    UUID atFromId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID sameTimeLowerId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    UUID sameTimeHigherId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    UUID atToId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    UUID afterId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    UUID inactiveId = UUID.fromString("00000000-0000-0000-0000-000000000006");

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
        atToId,
        completedTo,
        true,
        "040490-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000004"),
        "Other");
    insertMonitoringRow(
        afterId,
        Instant.parse("2026-09-18T10:06:00Z"),
        true,
        "050590-123A",
        UUID.fromString("10000000-0000-0000-0000-000000000005"),
        "Other");
    insertMonitoringRow(
        inactiveId,
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

  @Test
  void repeatsIdempotentlyAndRejectsDifferentPayload() throws Exception {
    UUID clientRequestId = UUID.randomUUID();
    String originalRequest = createRequest(clientRequestId, "010190-123A", "NewConsumerCredit");

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(originalRequest))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(originalRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.creditExtractSummary").isNotEmpty());

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class))
        .isEqualTo(1);
    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());

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

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class))
        .isEqualTo(1);
    verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
  }

  @Test
  void doesNotPersistAnythingWhenPcrTimesOut() throws Exception {
    UUID clientRequestId = UUID.randomUUID();
    doThrow(new PositiveCreditRegisterException(PositiveCreditRegisterException.Kind.TIMEOUT))
        .when(positiveCreditRegisterClient)
        .requestCreditExtract(any(), any());

    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(createRequest(clientRequestId, "010190-123A", "NewConsumerCredit")))
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

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class))
        .isZero();
  }

  private static String createRequest(
      UUID clientRequestId, String personalIdentityCode, String purpose) {
    return """
				{
				  "clientRequestId": "%s",
				  "personalIdentityCode": "%s",
				  "creditRegisterExtractPurposes": ["%s"]
				}
				"""
        .formatted(clientRequestId, personalIdentityCode, purpose);
  }
}
