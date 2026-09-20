package com.creditlens.backend.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface FinancingRequestHistoryProjection {
  UUID getId();

  UUID getClientRequestId();

  String getPersonalIdentityCode();

  Instant getRequestedAt();

  Instant getCompletedAt();

  UUID getExtractReference();

  boolean isVoluntaryCreditBanActive();
}
