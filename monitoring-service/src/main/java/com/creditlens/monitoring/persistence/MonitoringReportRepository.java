package com.creditlens.monitoring.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MonitoringReportRepository extends JpaRepository<MonitoringReportEntity, UUID> {
  Optional<MonitoringReportEntity> findFirstByStatusInOrderByCreatedAtAsc(
      Collection<MonitoringReportStatus> statuses);

  Optional<MonitoringReportEntity> findByIntervalStartAndIntervalEnd(
      Instant intervalStart, Instant intervalEnd);

  @Query("select max(report.intervalEnd) from MonitoringReportEntity report")
  Instant findLatestIntervalEnd();
}
