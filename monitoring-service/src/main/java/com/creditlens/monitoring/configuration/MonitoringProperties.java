package com.creditlens.monitoring.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("monitoring")
public record MonitoringProperties(String cron, Duration initialLookback, String recipient) {}
