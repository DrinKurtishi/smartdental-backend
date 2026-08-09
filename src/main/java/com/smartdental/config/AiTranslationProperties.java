package com.smartdental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartdental.ai.translation")
public record AiTranslationProperties(
        String provider, String apiKey, String model, String baseUrl, boolean enabled, long timeoutMs) {}
