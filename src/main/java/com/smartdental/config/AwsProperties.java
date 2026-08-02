package com.smartdental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "smartdental.aws")
public record AwsProperties(String region, @NestedConfigurationProperty Ses ses) {

    public record Ses(String sender, String accessKey, String secretKey, boolean enabled) {}
}
