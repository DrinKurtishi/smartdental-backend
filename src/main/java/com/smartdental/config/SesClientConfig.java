package com.smartdental.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesClientConfig {

    @Bean
    public SesClient sesClient(AwsProperties awsProperties) {
        AwsProperties.Ses ses = awsProperties.ses();
        AwsCredentialsProvider credentialsProvider =
                (ses.accessKey() != null && !ses.accessKey().isBlank())
                        ? StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(ses.accessKey(), ses.secretKey()))
                        : DefaultCredentialsProvider.create();

        return SesClient.builder()
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
