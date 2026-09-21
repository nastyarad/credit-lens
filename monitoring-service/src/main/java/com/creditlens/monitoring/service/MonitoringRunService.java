package com.creditlens.monitoring.service;

import com.creditlens.monitoring.configuration.MonitoringProperties;
import com.creditlens.monitoring.integration.backend.MonitoringBackendClient;
import com.creditlens.monitoring.integration.backend.MonitoringFinancingRequest;
import com.creditlens.monitoring.integration.mail.ReportMailSender;
import com.creditlens.monitoring.persistence.MonitoringReportEntity;
import com.creditlens.monitoring.persistence.MonitoringReportStatus;
import com.creditlens.monitoring.persistence.ReportPersistenceService;
import com.creditlens.monitoring.report.MonitoringReportRenderer;
import com.creditlens.monitoring.report.RenderedReport;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MonitoringRunService {
  private final ReportPersistenceService persistence;
  private final MonitoringBackendClient backend;
  private final MonitoringReportRenderer renderer;
  private final ReportMailSender mailSender;
  private final MonitoringProperties properties;
  private final Clock clock;

  public MonitoringRunService(
      ReportPersistenceService persistence,
      MonitoringBackendClient backend,
      MonitoringReportRenderer renderer,
      ReportMailSender mailSender,
      MonitoringProperties properties,
      Clock clock) {
    this.persistence = persistence;
    this.backend = backend;
    this.renderer = renderer;
    this.mailSender = mailSender;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(cron = "${monitoring.cron}", zone = "UTC")
  public void scheduledRun() {
    runOnce();
  }

  /** Executes one interval or one persisted delivery retry. */
  public void runOnce() {
    var undelivered = persistence.oldestUndelivered();
    if (undelivered.isPresent()) {
      deliver(undelivered.orElseThrow());
      return;
    }

    Instant currentRun = clock.instant();
    Instant previousRun = persistence.latestIntervalEnd();
    Instant intervalStart =
        previousRun == null ? currentRun.minus(properties.initialLookback()) : previousRun;
    List<MonitoringFinancingRequest> items =
        backend.findCompletedWithActiveBan(intervalStart, currentRun);
    RenderedReport rendered =
        renderer.render(properties.recipient(), intervalStart, currentRun, items);
    MonitoringReportEntity report =
        persistence.createOrFind(intervalStart, currentRun, rendered, items, currentRun);
    if (report.getStatus() != MonitoringReportStatus.SENT) deliver(report);
  }

  private void deliver(MonitoringReportEntity report) {
    Instant attemptedAt = clock.instant();
    try {
      mailSender.send(report);
      persistence.markSent(report.getId(), attemptedAt);
    } catch (RuntimeException exception) {
      persistence.markFailed(report.getId(), attemptedAt, safeMessage(exception));
    }
  }

  private static String safeMessage(RuntimeException exception) {
    String type = exception.getClass().getSimpleName();
    return type.isBlank() ? "SMTP delivery failed" : "SMTP delivery failed: " + type;
  }
}
