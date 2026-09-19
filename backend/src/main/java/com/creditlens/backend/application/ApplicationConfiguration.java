package com.creditlens.backend.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }
}
