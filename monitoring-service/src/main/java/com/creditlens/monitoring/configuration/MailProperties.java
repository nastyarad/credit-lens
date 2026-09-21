package com.creditlens.monitoring.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mail")
public record MailProperties(String sender) {}
