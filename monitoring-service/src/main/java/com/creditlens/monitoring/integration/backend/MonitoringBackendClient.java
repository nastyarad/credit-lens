package com.creditlens.monitoring.integration.backend;

import java.time.Instant;
import java.util.List;

public interface MonitoringBackendClient {
  List<MonitoringFinancingRequest> findCompletedWithActiveBan(
      Instant completedFrom, Instant completedTo);
}
