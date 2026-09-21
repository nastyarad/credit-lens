package com.creditlens.monitoring.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.monitoring.integration.backend.MonitoringFinancingRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonitoringReportRendererTest {
  private static final Instant START = Instant.parse("2026-09-21T10:00:00Z");
  private static final Instant END = Instant.parse("2026-09-21T10:05:00Z");

  @Test
  void rendersOnlyApprovedFieldsUsingMaskedIdentityCode() {
    RenderedReport rendered =
        new MonitoringReportRenderer()
            .render("pcr_monitoring@dansketest.dk", START, END, List.of(item()));

    assertThat(rendered.body())
        .contains("UTC interval: [2026-09-21T10:00:00Z, 2026-09-21T10:05:00Z)")
        .contains("Total records: 1")
        .contains("******-123A")
        .contains("ControlOfPersonalFinances")
        .doesNotContain("010190-123A")
        .doesNotContain("financingRequestId")
        .doesNotContain("extractReference")
        .doesNotContain(item().financingRequestId().toString());
  }

  @Test
  void rendersExplicitEmptyReport() {
    RenderedReport rendered =
        new MonitoringReportRenderer().render("recipient@example.test", START, END, List.of());

    assertThat(rendered.body())
        .contains("Total records: 0")
        .contains("No completed financing requests");
  }

  private static MonitoringFinancingRequest item() {
    return new MonitoringFinancingRequest(
        UUID.randomUUID(),
        START.minusSeconds(2),
        START.plusSeconds(2),
        "******-123A",
        "ControlOfPersonalFinances",
        2,
        3,
        0);
  }
}
