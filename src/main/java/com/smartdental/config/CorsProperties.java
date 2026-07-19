package com.smartdental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartdental.cors")
public record CorsProperties(String allowedOrigins) {}
