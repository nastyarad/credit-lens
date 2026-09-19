package com.creditlens.backend.integration.pcr;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pcr")
public record PcrProperties(
    String baseUrl,
    String targetEnvironment,
    Owner owner,
    Duration connectTimeout,
    Duration readTimeout) {

  public PcrProperties {
    baseUrl = baseUrl == null ? "http://localhost:8081" : baseUrl;
    targetEnvironment = targetEnvironment == null ? "Test" : targetEnvironment;
    owner = owner == null ? new Owner("BusinessId", "1234567-8", "FI") : owner;
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
    readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
  }

  public record Owner(String idCodeType, String idCode, String countryCode) {
    public Owner {
      idCodeType = idCodeType == null ? "BusinessId" : idCodeType;
      idCode = idCode == null ? "1234567-8" : idCode;
      countryCode = countryCode == null ? "FI" : countryCode;
    }
  }
}
