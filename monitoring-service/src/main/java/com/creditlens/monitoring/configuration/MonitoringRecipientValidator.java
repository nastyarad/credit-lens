package com.creditlens.monitoring.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class MonitoringRecipientValidator {
  static final String FIXED_RECIPIENT = "pcr_monitoring@dansketest.dk";

  private final MonitoringProperties properties;
  private final Environment environment;

  public MonitoringRecipientValidator(MonitoringProperties properties, Environment environment) {
    this.properties = properties;
    this.environment = environment;
  }

  @PostConstruct
  void validateRecipientOverride() {
    if (!FIXED_RECIPIENT.equals(properties.recipient())
        && !environment.acceptsProfiles(Profiles.of("local", "test", "real-mail"))) {
      throw new IllegalStateException(
          "MONITORING_RECIPIENT may be overridden only in local, test or real-mail");
    }
  }
}
