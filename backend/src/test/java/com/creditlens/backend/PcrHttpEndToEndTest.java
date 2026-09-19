package com.creditlens.backend;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class PcrHttpEndToEndTest {
  @RegisterExtension
  static final WireMockExtension wireMock =
      WireMockExtension.newInstance().options(options().dynamicPort()).build();

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

  @DynamicPropertySource
  static void pcrProperties(DynamicPropertyRegistry registry) {
    registry.add("pcr.base-url", wireMock::baseUrl);
  }

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetState() {
    jdbcTemplate.update("DELETE FROM credit_extract");
    jdbcTemplate.update("DELETE FROM financing_request");
    jdbcTemplate.update("DELETE FROM consumer");
    wireMock.resetAll();
    wireMock.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                okJson(
                    """
                    {"creditRegisterExtract":{"extractReference":"2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10","creationTimeUtc":"2026-09-18T10:15:30Z","personRequested":{"idCode":"010190-123A"},"creditInformationSummary":{"lendersCount":2,"loanContractsCount":1,"guaranteedLoanContractsCount":0},"repaymentsPaidLastAmount":[],"sumOfMonthlyLeasingInstalments":[],"loans":[],"incomeData":[]}}
                    """)));
  }

  @Test
  void persistsExtractReturnedByRealHttpClient() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/financing-requests")
                .contentType("application/json")
                .content(
                    """
                    {"clientRequestId":"ec2364ec-b4ed-4af7-805c-2bd44c42b9d5","personalIdentityCode":"010190-123A","creditRegisterExtractPurposes":["NewConsumerCredit"]}
                    """))
        .andExpect(status().isCreated());

    assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM credit_extract", Integer.class))
        .isEqualTo(1);
  }
}
