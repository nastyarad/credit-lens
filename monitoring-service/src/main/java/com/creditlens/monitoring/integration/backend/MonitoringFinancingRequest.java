package com.creditlens.monitoring.integration.backend;

import java.time.Instant;
import java.util.UUID;

public record MonitoringFinancingRequest(
    UUID financingRequestId,
    Instant requestedAt,
    Instant completedAt,
    String maskedPersonalIdentityCode,
    String voluntaryCreditBanReason,
    int lendersCount,
    int loanContractsCount,
    int guaranteedLoanContractsCount) {}
