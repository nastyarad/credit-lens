package com.creditlens.monitoring.integration.mail;

import com.creditlens.monitoring.configuration.MailProperties;
import com.creditlens.monitoring.persistence.MonitoringReportEntity;
import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpReportMailSender implements ReportMailSender {
  private final JavaMailSender mailSender;
  private final MailProperties properties;

  public SmtpReportMailSender(JavaMailSender mailSender, MailProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public void send(MonitoringReportEntity report) {
    try {
      MimeMessageHelper message = new MimeMessageHelper(mailSender.createMimeMessage(), "UTF-8");
      message.setFrom(properties.sender());
      message.setTo(report.getRecipient());
      message.setSubject(report.getSubject());
      message.setText(report.getBody(), false);
      mailSender.send(message.getMimeMessage());
    } catch (MessagingException exception) {
      throw new IllegalStateException("SMTP message could not be prepared", exception);
    }
  }
}
