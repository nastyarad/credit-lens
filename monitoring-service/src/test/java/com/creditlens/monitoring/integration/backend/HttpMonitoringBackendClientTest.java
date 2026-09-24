package com.creditlens.monitoring.integration.backend;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creditlens.monitoring.configuration.BackendProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpMonitoringBackendClientTest {
  private WireMockServer backend;
  private HttpMonitoringBackendClient client;
  private final Instant start = Instant.parse("2026-09-21T10:00:00Z");
  private final Instant end = Instant.parse("2026-09-21T10:05:00Z");

  @BeforeEach
  void setUp() {
    backend = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    backend.start();
    client =
        new HttpMonitoringBackendClient(
            RestClient.builder(),
            new BackendProperties(backend.baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1)));
  }

  @AfterEach
  void tearDown() {
    backend.stop();
  }

  @Test
  void loadsAllPagesWithSizeFiveHundredAndPreservesPageOrder() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0"))
            .willReturn(json(page(0, 0, 500, 501, 2))));
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
            .willReturn(json(page(1, 500, 1, 501, 2))));

    List<MonitoringFinancingRequest> results = client.findCompletedWithActiveBan(start, end);

    assertThat(results)
        .extracting(item -> item.financingRequestId().toString())
        .hasSize(501)
        .startsWith("00000000-0000-0000-0000-000000000000")
        .endsWith("00000000-0000-0000-0000-000000000500");
    backend.verify(
        2,
        getRequestedFor(urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam(
                "size", com.github.tomakehurst.wiremock.client.WireMock.equalTo("500")));
  }

  @Test
  void acceptsEmptyResponseFromBackend() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .willReturn(json(page(0, 0, 0, 0, 0))));

    List<MonitoringFinancingRequest> results = client.findCompletedWithActiveBan(start, end);

    assertThat(results).isEmpty();
    backend.verify(1, getRequestedFor(urlPathEqualTo("/api/v1/monitoring/financing-requests")));
  }

  @Test
  void rejectsInconsistentOrDuplicatePages() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .willReturn(json(page(1, 0, 1, 1, 1))));

    assertThatThrownBy(() -> client.findCompletedWithActiveBan(start, end))
        .isInstanceOf(BackendMonitoringException.class);
  }

  @Test
  void rejectsIncompletePagesEvenWhenTheirCombinedItemCountMatchesTotal() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0"))
            .willReturn(json(page(0, 0, 499, 501, 2))));
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
            .willReturn(json(page(1, 500, 2, 501, 2))));

    assertThatThrownBy(() -> client.findCompletedWithActiveBan(start, end))
        .isInstanceOf(BackendMonitoringException.class)
        .hasMessage("backend pagination was incomplete");
  }

  @Test
  void rejectsItemsOutOfGlobalStableOrderAcrossPages() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0"))
            .willReturn(json(page(0, 0, 500, 501, 2))));
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .withQueryParam("page", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1"))
            .willReturn(json(page(1, 500, 1, 501, 2, "2026-09-21T10:00:59Z"))));

    assertThatThrownBy(() -> client.findCompletedWithActiveBan(start, end))
        .isInstanceOf(BackendMonitoringException.class)
        .hasMessage("backend returned items in an unstable order");
  }

  @Test
  void rejectsItemAtExclusiveIntervalEnd() {
    backend.stubFor(
        com.github.tomakehurst.wiremock.client.WireMock.get(
                urlPathEqualTo("/api/v1/monitoring/financing-requests"))
            .willReturn(json(singleItemPage(end))));

    assertThatThrownBy(() -> client.findCompletedWithActiveBan(start, end))
        .isInstanceOf(BackendMonitoringException.class);
  }

  private static String page(
      int page, int firstId, int itemCount, long totalItems, int totalPages) {
    return page(page, firstId, itemCount, totalItems, totalPages, "2026-09-21T10:01:00Z");
  }

  private static String page(
      int page, int firstId, int itemCount, long totalItems, int totalPages, String completedAt) {
    String items =
        java.util.stream.IntStream.range(firstId, firstId + itemCount)
            .mapToObj(id -> itemJson(id, completedAt))
            .collect(java.util.stream.Collectors.joining(","));
    return "{\"items\":[%s],\"page\":%d,\"size\":500,\"totalItems\":%d,\"totalPages\":%d}"
        .formatted(items, page, totalItems, totalPages);
  }

  private static String itemJson(int id) {
    return itemJson(id, "2026-09-21T10:01:00Z");
  }

  private static String itemJson(int id, String completedAt) {
    return """
        {"financingRequestId":"00000000-0000-0000-0000-%012d","requestedAt":"2026-09-21T09:59:00Z",
        "completedAt":"%s","maskedPersonalIdentityCode":"******-123A",
        "voluntaryCreditBanReason":"ControlOfPersonalFinances","lendersCount":1,
        "loanContractsCount":2,"guaranteedLoanContractsCount":0}
        """
        .formatted(id, completedAt)
        .replace("\n", "");
  }

  private static String singleItemPage(Instant completedAt) {
    return """
        {"items":[{"financingRequestId":"11111111-1111-1111-1111-111111111111",
        "requestedAt":"2026-09-21T09:59:00Z","completedAt":"%s",
        "maskedPersonalIdentityCode":"******-123A",
        "voluntaryCreditBanReason":"ControlOfPersonalFinances","lendersCount":1,
        "loanContractsCount":2,"guaranteedLoanContractsCount":0}],"page":0,"size":500,
        "totalItems":1,"totalPages":1}
        """
        .formatted(completedAt)
        .replace("\n", "");
  }

  private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
      String body) {
    return aResponse().withHeader("Content-Type", "application/json").withBody(body);
  }
}
