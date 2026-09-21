package com.creditlens.monitoring.persistence;

import com.creditlens.monitoring.integration.backend.MonitoringFinancingRequest;
import com.creditlens.monitoring.report.RenderedReport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReportPersistenceService {
  private final MonitoringReportRepository reports;
  private final TransactionTemplate transactions;

  public ReportPersistenceService(
      MonitoringReportRepository reports, PlatformTransactionManager transactionManager) {
    this.reports = reports;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  public Optional<MonitoringReportEntity> oldestUndelivered() {
    return reports.findFirstByStatusInOrderByCreatedAtAsc(
        List.of(MonitoringReportStatus.CREATED, MonitoringReportStatus.FAILED));
  }

  public Instant latestIntervalEnd() {
    return reports.findLatestIntervalEnd();
  }

  public MonitoringReportEntity createOrFind(
      Instant intervalStart,
      Instant intervalEnd,
      RenderedReport rendered,
      List<MonitoringFinancingRequest> sourceItems,
      Instant createdAt) {
    Optional<MonitoringReportEntity> existing =
        reports.findByIntervalStartAndIntervalEnd(intervalStart, intervalEnd);
    if (existing.isPresent()) return existing.orElseThrow();
    List<MonitoringReportItemEntity> items =
        java.util.stream.IntStream.range(0, sourceItems.size())
            .mapToObj(index -> toEntity(index, sourceItems.get(index)))
            .toList();
    try {
      return java.util.Objects.requireNonNull(
          transactions.execute(
              status ->
                  reports.saveAndFlush(
                      new MonitoringReportEntity(
                          UUID.randomUUID(),
                          intervalStart,
                          intervalEnd,
                          rendered.recipient(),
                          rendered.subject(),
                          rendered.body(),
                          createdAt,
                          items))));
    } catch (DataIntegrityViolationException exception) {
      return reports
          .findByIntervalStartAndIntervalEnd(intervalStart, intervalEnd)
          .orElseThrow(() -> exception);
    }
  }

  @Transactional
  public void markSent(UUID reportId, Instant attemptedAt) {
    reports.findById(reportId).orElseThrow().markSent(attemptedAt);
  }

  @Transactional
  public void markFailed(UUID reportId, Instant attemptedAt, String safeError) {
    reports.findById(reportId).orElseThrow().markFailed(attemptedAt, safeError);
  }

  private MonitoringReportItemEntity toEntity(int ordinal, MonitoringFinancingRequest item) {
    return new MonitoringReportItemEntity(
        ordinal,
        item.maskedPersonalIdentityCode(),
        item.requestedAt(),
        item.completedAt(),
        item.voluntaryCreditBanReason(),
        item.lendersCount(),
        item.loanContractsCount(),
        item.guaranteedLoanContractsCount());
  }
}
