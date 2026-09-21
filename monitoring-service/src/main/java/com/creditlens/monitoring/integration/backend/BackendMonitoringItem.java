package com.creditlens.monitoring.integration.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
record BackendMonitoringItem(
    UUID financingRequestId,
    Instant requestedAt,
    Instant completedAt,
    String maskedPersonalIdentityCode,
    String voluntaryCreditBanReason,
    int lendersCount,
    int loanContractsCount,
    int guaranteedLoanContractsCount) {
  MonitoringFinancingRequest toDomain() {
    return new MonitoringFinancingRequest(
        financingRequestId,
        requestedAt,
        completedAt,
        maskedPersonalIdentityCode,
        voluntaryCreditBanReason,
        lendersCount,
        loanContractsCount,
        guaranteedLoanContractsCount);
  }
}
