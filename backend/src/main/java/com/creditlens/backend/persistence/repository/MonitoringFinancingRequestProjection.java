package com.creditlens.backend.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface MonitoringFinancingRequestProjection {
  UUID getFinancingRequestId();

  UUID getExtractReference();

  Instant getRequestedAt();

  Instant getCompletedAt();

  String getPersonalIdentityCode();

  String getVoluntaryCreditBanReason();

  int getLendersCount();

  int getLoanContractsCount();

  int getGuaranteedLoanContractsCount();
}
