package com.creditlens.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creditlens.monitoring.configuration.MonitoringProperties;
import com.creditlens.monitoring.integration.backend.BackendMonitoringException;
import com.creditlens.monitoring.integration.backend.MonitoringBackendClient;
import com.creditlens.monitoring.integration.backend.MonitoringFinancingRequest;
import com.creditlens.monitoring.integration.mail.ReportMailSender;
import com.creditlens.monitoring.persistence.MonitoringReportEntity;
import com.creditlens.monitoring.persistence.MonitoringReportStatus;
import com.creditlens.monitoring.persistence.ReportPersistenceService;
import com.creditlens.monitoring.report.MonitoringReportRenderer;
import com.creditlens.monitoring.report.RenderedReport;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonitoringRunServiceTest {
  private static final Instant NOW = Instant.parse("2026-09-21T10:05:00Z");

  @Test
  void firstRunUsesInitialLookbackAndPersistsThenSendsEmptyReport() {
    ReportPersistenceService persistence = persistence();
    MonitoringBackendClient backend = mock(MonitoringBackendClient.class);
    ReportMailSender mail = mock(ReportMailSender.class);
    when(persistence.oldestUndelivered()).thenReturn(Optional.empty());
    when(persistence.latestIntervalEnd()).thenReturn(null);
    when(backend.findCompletedWithActiveBan(any(), any())).thenReturn(List.of());
    MonitoringReportEntity report = report(NOW.minus(Duration.ofMinutes(5)), NOW);
    when(persistence.createOrFind(any(), any(), any(), any(), any())).thenReturn(report);

    service(persistence, backend, mail).runOnce();

    verify(backend).findCompletedWithActiveBan(NOW.minus(Duration.ofMinutes(5)), NOW);
    ArgumentCaptor<RenderedReport> rendered = ArgumentCaptor.forClass(RenderedReport.class);
    verify(persistence)
        .createOrFind(
            eq(NOW.minus(Duration.ofMinutes(5))),
            eq(NOW),
            rendered.capture(),
            eq(List.of()),
            eq(NOW));
    assertThat(rendered.getValue().body()).contains("Total records: 0");
    verify(mail).send(report);
    verify(persistence).markSent(report.getId(), NOW);
  }

  @Test
  void nextRunStartsExactlyAtPreviousIntervalEnd() {
    ReportPersistenceService persistence = persistence();
    MonitoringBackendClient backend = mock(MonitoringBackendClient.class);
    ReportMailSender mail = mock(ReportMailSender.class);
    Instant previousEnd = Instant.parse("2026-09-21T10:00:00Z");
    when(persistence.oldestUndelivered()).thenReturn(Optional.empty());
    when(persistence.latestIntervalEnd()).thenReturn(previousEnd);
    when(backend.findCompletedWithActiveBan(previousEnd, NOW)).thenReturn(List.of(item()));
    when(persistence.createOrFind(any(), any(), any(), any(), any()))
        .thenReturn(report(previousEnd, NOW));

    service(persistence, backend, mail).runOnce();

    verify(backend).findCompletedWithActiveBan(previousEnd, NOW);
  }

  @Test
  void retriesSavedContentWithoutCallingBackendAfterSmtpFailure() {
    ReportPersistenceService persistence = persistence();
    MonitoringBackendClient backend = mock(MonitoringBackendClient.class);
    ReportMailSender mail = mock(ReportMailSender.class);
    MonitoringReportEntity saved = report(NOW.minusSeconds(300), NOW);
    when(persistence.oldestUndelivered()).thenReturn(Optional.of(saved));
    doThrow(new IllegalStateException("mailpit unavailable")).when(mail).send(saved);

    service(persistence, backend, mail).runOnce();

    verify(mail).send(saved);
    verify(backend, never()).findCompletedWithActiveBan(any(), any());
    verify(persistence)
        .markFailed(saved.getId(), NOW, "SMTP delivery failed: IllegalStateException");
  }

  @Test
  void backendFailureDoesNotCreateReportOrAdvanceCheckpoint() {
    ReportPersistenceService persistence = persistence();
    MonitoringBackendClient backend = mock(MonitoringBackendClient.class);
    ReportMailSender mail = mock(ReportMailSender.class);
    when(persistence.oldestUndelivered()).thenReturn(Optional.empty());
    when(persistence.latestIntervalEnd()).thenReturn(null);
    when(backend.findCompletedWithActiveBan(any(), any()))
        .thenThrow(new BackendMonitoringException("unavailable"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service(persistence, backend, mail).runOnce())
        .isInstanceOf(BackendMonitoringException.class);

    verify(persistence, never()).createOrFind(any(), any(), any(), any(), any());
    verify(mail, never()).send(any());
  }

  @Test
  void doesNotSendAgainWhenConcurrentIntervalCreationReturnsSentReport() {
    ReportPersistenceService persistence = persistence();
    MonitoringBackendClient backend = mock(MonitoringBackendClient.class);
    ReportMailSender mail = mock(ReportMailSender.class);
    when(persistence.oldestUndelivered()).thenReturn(Optional.empty());
    when(persistence.latestIntervalEnd()).thenReturn(NOW.minus(Duration.ofMinutes(5)));
    when(backend.findCompletedWithActiveBan(any(), any())).thenReturn(List.of());
    MonitoringReportEntity sent = report(NOW.minus(Duration.ofMinutes(5)), NOW);
    sent.markSent(NOW);
    when(persistence.createOrFind(any(), any(), any(), any(), any())).thenReturn(sent);

    service(persistence, backend, mail).runOnce();

    verify(mail, never()).send(any());
    assertThat(sent.getStatus()).isEqualTo(MonitoringReportStatus.SENT);
  }

  private static MonitoringRunService service(
      ReportPersistenceService persistence,
      MonitoringBackendClient backend,
      ReportMailSender mail) {
    return new MonitoringRunService(
        persistence,
        backend,
        new MonitoringReportRenderer(),
        mail,
        new MonitoringProperties(
            "0 * * * * *", Duration.ofMinutes(5), "pcr_monitoring@dansketest.dk"),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static ReportPersistenceService persistence() {
    return mock(ReportPersistenceService.class);
  }

  private static MonitoringReportEntity report(Instant start, Instant end) {
    return new MonitoringReportEntity(
        UUID.randomUUID(),
        start,
        end,
        "pcr_monitoring@dansketest.dk",
        "saved subject",
        "saved body",
        start,
        List.of());
  }

  private static MonitoringFinancingRequest item() {
    return new MonitoringFinancingRequest(
        UUID.randomUUID(),
        NOW.minusSeconds(10),
        NOW.minusSeconds(1),
        "******-123A",
        "ControlOfPersonalFinances",
        1,
        2,
        0);
  }
}
