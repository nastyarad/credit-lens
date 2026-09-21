package com.creditlens.monitoring.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("backend")
public record BackendProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {}
