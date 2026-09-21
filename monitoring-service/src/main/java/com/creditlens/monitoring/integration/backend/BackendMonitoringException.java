package com.creditlens.monitoring.integration.backend;

public class BackendMonitoringException extends RuntimeException {
  public BackendMonitoringException(String message, Throwable cause) {
    super(message, cause);
  }

  public BackendMonitoringException(String message) {
    super(message);
  }
}
