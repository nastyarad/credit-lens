package com.creditlens.monitoring.integration.backend;

import com.creditlens.monitoring.configuration.BackendProperties;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpMonitoringBackendClient implements MonitoringBackendClient {
  private static final int PAGE_SIZE = 500;

  private final RestClient restClient;

  public HttpMonitoringBackendClient(RestClient.Builder builder, BackendProperties properties) {
    this.restClient =
        builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory(properties)).build();
  }

  @Override
  public List<MonitoringFinancingRequest> findCompletedWithActiveBan(
      Instant completedFrom, Instant completedTo) {
    try {
      BackendMonitoringResponse first = getPage(completedFrom, completedTo, 0);
      validate(first, 0, first);
      List<MonitoringFinancingRequest> result = new ArrayList<>();
      addItems(result, first.items(), completedFrom, completedTo);
      for (int page = 1; page < first.totalPages(); page++) {
        BackendMonitoringResponse next = getPage(completedFrom, completedTo, page);
        validate(next, page, first);
        addItems(result, next.items(), completedFrom, completedTo);
      }
      if (result.size() != first.totalItems()) {
        throw new BackendMonitoringException("backend pagination was incomplete");
      }
      assertNoDuplicates(result);
      assertStableOrder(result);
      return List.copyOf(result);
    } catch (RestClientException exception) {
      throw new BackendMonitoringException("backend monitoring request failed", exception);
    }
  }

  private BackendMonitoringResponse getPage(Instant from, Instant to, int page) {
    BackendMonitoringResponse response =
        restClient
            .get()
            .uri(
                builder ->
                    builder
                        .path("/api/v1/monitoring/financing-requests")
                        .queryParam("completedFrom", from)
                        .queryParam("completedTo", to)
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .build())
            .retrieve()
            .body(BackendMonitoringResponse.class);
    if (response == null)
      throw new BackendMonitoringException("backend returned an empty response");
    return response;
  }

  private static void validate(
      BackendMonitoringResponse response, int expectedPage, BackendMonitoringResponse first) {
    if (response.items() == null
        || response.page() != expectedPage
        || response.size() != PAGE_SIZE
        || response.totalItems() < 0
        || response.totalPages() < 0
        || response.totalItems() != first.totalItems()
        || response.totalPages() != first.totalPages()) {
      throw new BackendMonitoringException("backend returned invalid pagination metadata");
    }
    int expectedPages =
        response.totalItems() == 0
            ? 0
            : Math.toIntExact((response.totalItems() + PAGE_SIZE - 1) / PAGE_SIZE);
    if (response.totalPages() != expectedPages) {
      throw new BackendMonitoringException("backend returned inconsistent pagination metadata");
    }
    if (response.totalItems() == 0) {
      if (expectedPage != 0 || !response.items().isEmpty()) {
        throw new BackendMonitoringException("backend returned inconsistent pagination metadata");
      }
      return;
    }
    if (expectedPage >= response.totalPages()) {
      throw new BackendMonitoringException("backend returned inconsistent pagination metadata");
    }
    long remainingItems = response.totalItems() - (long) expectedPage * PAGE_SIZE;
    int expectedItemCount = (int) Math.min(PAGE_SIZE, remainingItems);
    if (response.items().size() != expectedItemCount) {
      throw new BackendMonitoringException("backend pagination was incomplete");
    }
  }

  private static void addItems(
      List<MonitoringFinancingRequest> target,
      List<BackendMonitoringItem> items,
      Instant completedFrom,
      Instant completedTo) {
    for (BackendMonitoringItem item : items) {
      if (item == null
          || item.financingRequestId() == null
          || item.requestedAt() == null
          || item.completedAt() == null
          || item.maskedPersonalIdentityCode() == null
          || item.voluntaryCreditBanReason() == null
          || item.lendersCount() < 0
          || item.loanContractsCount() < 0
          || item.guaranteedLoanContractsCount() < 0
          || item.completedAt().isBefore(completedFrom)
          || !item.completedAt().isBefore(completedTo)) {
        throw new BackendMonitoringException("backend returned an incomplete monitoring item");
      }
      target.add(item.toDomain());
    }
  }

  private static void assertNoDuplicates(List<MonitoringFinancingRequest> items) {
    Set<UUID> ids = new HashSet<>();
    for (MonitoringFinancingRequest item : items) {
      if (!ids.add(item.financingRequestId())) {
        throw new BackendMonitoringException("backend returned duplicate financing request IDs");
      }
    }
  }

  private static void assertStableOrder(List<MonitoringFinancingRequest> items) {
    for (int index = 1; index < items.size(); index++) {
      MonitoringFinancingRequest previous = items.get(index - 1);
      MonitoringFinancingRequest current = items.get(index);
      if (previous.completedAt().isAfter(current.completedAt())
          || (previous.completedAt().equals(current.completedAt())
              && previous.financingRequestId().compareTo(current.financingRequestId()) > 0)) {
        throw new BackendMonitoringException("backend returned items in an unstable order");
      }
    }
  }

  private static JdkClientHttpRequestFactory requestFactory(BackendProperties properties) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(properties.readTimeout());
    return factory;
  }
}
