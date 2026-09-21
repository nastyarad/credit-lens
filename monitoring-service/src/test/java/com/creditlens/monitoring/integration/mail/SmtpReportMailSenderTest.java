package com.creditlens.monitoring.integration.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.creditlens.monitoring.configuration.MailProperties;
import com.creditlens.monitoring.persistence.MonitoringReportEntity;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpReportMailSenderTest {
  @RegisterExtension
  static final GreenMailExtension SMTP = new GreenMailExtension(ServerSetupTest.SMTP);

  @Test
  void deliversPersistedPlainTextReportOverSmtp() throws Exception {
    JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
    javaMailSender.setHost("localhost");
    javaMailSender.setPort(SMTP.getSmtp().getPort());
    MonitoringReportEntity report =
        new MonitoringReportEntity(
            UUID.randomUUID(),
            Instant.parse("2026-09-21T10:00:00Z"),
            Instant.parse("2026-09-21T10:05:00Z"),
            "recipient@example.test",
            "saved subject",
            "saved body",
            Instant.parse("2026-09-21T10:05:00Z"),
            List.of());

    new SmtpReportMailSender(javaMailSender, new MailProperties("sender@example.test"))
        .send(report);

    assertThat(SMTP.waitForIncomingEmail(1)).isTrue();
    assertThat(SMTP.getReceivedMessages()[0].getSubject()).isEqualTo("saved subject");
    assertThat(SMTP.getReceivedMessages()[0].getContent()).isEqualTo("saved body");
  }
}
