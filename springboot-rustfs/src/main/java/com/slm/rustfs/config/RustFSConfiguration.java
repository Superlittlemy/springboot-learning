package com.slm.rustfs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class RustFSConfiguration {

    @Autowired
    private RustFSProperties properties;

    @Bean
    public S3Client client() {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint())) // RustFS 地址
                .region(Region.US_EAST_1) // 可写死，RustFS 不校验 region
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
                ))
                .forcePathStyle(true) // 关键配置！RustFS 需启用 Path-Style
                .build();
    }

    @Bean
    public S3Presigner presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()))
                )
                // 关键：显式设置 path style
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build())
                .build();
    }

}
