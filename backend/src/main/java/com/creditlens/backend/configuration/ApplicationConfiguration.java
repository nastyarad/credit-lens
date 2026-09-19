package com.creditlens.backend.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }
}
