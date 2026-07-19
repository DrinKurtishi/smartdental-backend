package com.smartdental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartdental.frontend")
public record FrontendProperties(String baseUrl) {}
