package com.creditlens.backend.support;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.Mockito.clearInvocations;

import com.creditlens.backend.integration.pcr.PositiveCreditRegisterClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BackendApiIntegrationTestSupport {

  protected static final WireMockServer wireMock =
      new WireMockServer(options().dynamicPort().usingFilesUnderClasspath("wiremock"));
  protected static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine");

  static {
    postgres.start();
    wireMock.start();
  }

  @DynamicPropertySource
  static void pcrProperties(DynamicPropertyRegistry registry) {
    registry.add("pcr.base-url", wireMock::baseUrl);
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired protected JdbcTemplate jdbcTemplate;
  @Autowired protected MockMvc mockMvc;

  @MockitoSpyBean protected PositiveCreditRegisterClient positiveCreditRegisterClient;

  @BeforeEach
  void resetBackend() {
    jdbcTemplate.update("DELETE FROM credit_extract");
    jdbcTemplate.update("DELETE FROM financing_request");
    jdbcTemplate.update("DELETE FROM consumer");
    clearInvocations(positiveCreditRegisterClient);
    wireMock.resetAll();
    stubPcrResponse("pcr-empty-extract.json");
  }

  protected void stubPcrResponse(String bodyFile) {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json").withBodyFile(bodyFile)));
  }

  protected static String createRequest(
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
