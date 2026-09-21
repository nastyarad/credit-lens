package com.creditlens.backend.integration.pcr;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creditlens.backend.domain.CreditRegisterExtractPurpose;
import com.creditlens.backend.domain.PersonalIdentityCode;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

class HttpPositiveCreditRegisterClientIntegrationTest {

  @RegisterExtension
  static final WireMockExtension wireMock =
      WireMockExtension.newInstance()
          .options(options().dynamicPort().usingFilesUnderClasspath("wiremock"))
          .build();

  private HttpPositiveCreditRegisterClient client;

  @BeforeEach
  void setUp() {
    client = clientWithReadTimeout(Duration.ofSeconds(2));
  }

  private HttpPositiveCreditRegisterClient clientWithReadTimeout(Duration readTimeout) {
    PcrProperties properties =
        new PcrProperties(
            wireMock.baseUrl(),
            "Test",
            new PcrProperties.Owner("BusinessId", "1234567-8", "FI"),
            Duration.ofSeconds(1),
            readTimeout);
    return new HttpPositiveCreditRegisterClient(RestClient.builder(), properties);
  }

  @Test
  void sendsPcrRequestAndMapsSuccessfulResponse() {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withBodyFile("pcr-complete-extract.json")));

    var extract =
        client.requestCreditExtract(
            PersonalIdentityCode.of("010190-123A"),
            List.of(CreditRegisterExtractPurpose.NewConsumerCredit));

    assertThat(extract.extractReference().toString())
        .isEqualTo("55555555-5555-5555-5555-555555555555");
    assertThat(extract.creditInformationSummary().lendersCount()).isEqualTo(2);
    assertThat(extract.loans()).hasSize(1);
    assertThat(extract.incomeData()).hasSize(1);
    wireMock.verify(
        1,
        postRequestedFor(urlEqualTo("/GetCreditRegisterExtract"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(matchingJsonPath("$.targetEnvironment", equalTo("Test")))
            .withRequestBody(matchingJsonPath("$.owner.idCode", equalTo("1234567-8")))
            .withRequestBody(matchingJsonPath("$.idCode", equalTo("010190-123A")))
            .withRequestBody(
                matchingJsonPath(
                    "$.creditRegisterExtractPurpose[0]", equalTo("NewConsumerCredit"))));
  }

  @Test
  void mapsRejectedResponseWithoutExposingUpstreamPayload() {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"errorResponses\":[{\"errorDescription\":\"sensitive\"}]}")));

    assertThatThrownBy(this::requestExtract)
        .isInstanceOfSatisfying(
            PositiveCreditRegisterException.class,
            exception -> {
              assertThat(exception.kind()).isEqualTo(PositiveCreditRegisterException.Kind.REJECTED);
              assertThat(exception.getMessage()).doesNotContain("sensitive");
            });
  }

  @Test
  void mapsMalformedSuccessfulResponseToInvalidResponse() {
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(ok().withHeader("Content-Type", "application/json").withBody("{not-json")));

    assertThatThrownBy(this::requestExtract)
        .isInstanceOfSatisfying(
            PositiveCreditRegisterException.class,
            exception ->
                assertThat(exception.kind())
                    .isEqualTo(PositiveCreditRegisterException.Kind.INVALID_RESPONSE));
  }

  @Test
  void mapsReadTimeoutToTimeout() {
    client = clientWithReadTimeout(Duration.ofMillis(100));
    wireMock.stubFor(
        post(urlEqualTo("/GetCreditRegisterExtract"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withFixedDelay(500)
                    .withBodyFile("pcr-empty-extract.json")));

    assertThatThrownBy(this::requestExtract)
        .isInstanceOfSatisfying(
            PositiveCreditRegisterException.class,
            exception ->
                assertThat(exception.kind())
                    .isEqualTo(PositiveCreditRegisterException.Kind.TIMEOUT));
  }

  private void requestExtract() {
    client.requestCreditExtract(
        PersonalIdentityCode.of("010190-123A"),
        List.of(CreditRegisterExtractPurpose.NewConsumerCredit));
  }
}
