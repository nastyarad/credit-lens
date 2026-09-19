package com.creditlens.backend;

import com.creditlens.backend.integration.pcr.PositiveCreditRegisterClient;
import com.creditlens.backend.integration.pcr.PositiveCreditRegisterException;
import com.creditlens.backend.persistence.entity.CreditExtractEntity;
import com.creditlens.backend.persistence.repository.CreditExtractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	CreditExtractRepository creditExtractRepository;

	@MockitoSpyBean
	PositiveCreditRegisterClient positiveCreditRegisterClient;

	@BeforeEach
	void cleanBackendTables() {
		jdbcTemplate.update("DELETE FROM credit_extract");
		jdbcTemplate.update("DELETE FROM financing_request");
		jdbcTemplate.update("DELETE FROM consumer");
		clearInvocations(positiveCreditRegisterClient);
	}

	@Test
	void appliesDatabaseMigration() {
		Integer appliedMigrationCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM flyway_schema_history
				WHERE version = '1' AND success = TRUE
				""", Integer.class);

		assertThat(appliedMigrationCount).isEqualTo(1);
	}

	@Test
	void createsCompletedRequestAndPersistsSchemaCompatibleData() throws Exception {
		UUID clientRequestId = UUID.randomUUID();
		String personalIdentityCode = "010190-123A";

		doAnswer(invocation -> {
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
			return invocation.callRealMethod();
		}).when(positiveCreditRegisterClient).requestCreditExtract(any(), any());

		String response = mockMvc.perform(post("/api/v1/financing-requests")
					.contentType("application/json")
					.content(createRequest(clientRequestId, personalIdentityCode, "NewConsumerCredit")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
						"/api/v1/financing-requests/[0-9a-f-]+"
				)))
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
				.andExpect(jsonPath("$.creditExtractSummary.voluntaryBanOnCredits.isInEffect").value(false))
				.andExpect(jsonPath("$.creditExtractSummary.voluntaryBanOnCredits.reason").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(response).doesNotContain(personalIdentityCode);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class)).isEqualTo(1);

		Map<String, Object> requestRow = jdbcTemplate.queryForMap("""
				SELECT requested_at, completed_at
				FROM financing_request
				WHERE client_request_id = ?
				""", clientRequestId);
		Instant requestedAt = ((java.sql.Timestamp) requestRow.get("requested_at")).toInstant();
		Instant completedAt = ((java.sql.Timestamp) requestRow.get("completed_at")).toInstant();
		assertThat(completedAt).isAfterOrEqualTo(requestedAt);

		Map<String, Object> jsonFields = jdbcTemplate.queryForMap("""
				SELECT repayment_amounts::text AS repayments,
				       leasing_instalment_amounts::text AS leasing,
				       loans::text AS loans,
				       income_data::text AS income
				FROM credit_extract
				""");
		assertThat(jsonFields).containsEntry("repayments", "[]")
				.containsEntry("leasing", "[]")
				.containsEntry("loans", "[]")
				.containsEntry("income", "[]");

		UUID financingRequestId = jdbcTemplate.queryForObject(
				"SELECT id FROM financing_request WHERE client_request_id = ?",
				UUID.class,
				clientRequestId
		);
		CreditExtractEntity reloadedExtract = creditExtractRepository
				.findByFinancingRequest_Id(financingRequestId)
				.orElseThrow();
		assertThat(reloadedExtract.getRepaymentAmounts()).isEmpty();
		assertThat(reloadedExtract.getLeasingInstalmentAmounts()).isEmpty();
		assertThat(reloadedExtract.getLoans()).isEmpty();
		assertThat(reloadedExtract.getIncomeData()).isEmpty();
		verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
	}

	@Test
	void repeatsIdempotentlyAndRejectsDifferentPayload() throws Exception {
		UUID clientRequestId = UUID.randomUUID();
		String originalRequest = createRequest(clientRequestId, "010190-123A", "NewConsumerCredit");

		mockMvc.perform(post("/api/v1/financing-requests")
					.contentType("application/json")
					.content(originalRequest))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/financing-requests")
					.contentType("application/json")
					.content(originalRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.creditExtractSummary").isNotEmpty());

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class)).isEqualTo(1);
		verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());

		mockMvc.perform(post("/api/v1/financing-requests")
					.contentType("application/json")
					.content(createRequest(clientRequestId, "010190-123A", "NewLoan")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("clientRequestId was already used with different input"));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class)).isEqualTo(1);
		verify(positiveCreditRegisterClient, times(1)).requestCreditExtract(any(), any());
	}

	@Test
	void doesNotPersistAnythingWhenPcrTimesOut() throws Exception {
		UUID clientRequestId = UUID.randomUUID();
		doThrow(new PositiveCreditRegisterException(PositiveCreditRegisterException.Kind.TIMEOUT))
				.when(positiveCreditRegisterClient).requestCreditExtract(any(), any());

		mockMvc.perform(post("/api/v1/financing-requests")
				.contentType("application/json")
				.content(createRequest(clientRequestId, "010190-123A", "NewConsumerCredit")))
				.andExpect(status().isGatewayTimeout())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(504))
				.andExpect(jsonPath("$.detail").value(
						"The Positive Credit Register request could not be completed."))
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("010190-123A"))));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumer", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM financing_request", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class)).isZero();
	}

	private static String createRequest(UUID clientRequestId, String personalIdentityCode, String purpose) {
		return """
				{
				  "clientRequestId": "%s",
				  "personalIdentityCode": "%s",
				  "creditRegisterExtractPurposes": ["%s"]
				}
				""".formatted(clientRequestId, personalIdentityCode, purpose);
	}

}
