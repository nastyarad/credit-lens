package com.creditlens.monitoring.integration.mail;

import com.creditlens.monitoring.persistence.MonitoringReportEntity;

public interface ReportMailSender {
  void send(MonitoringReportEntity report);
}
