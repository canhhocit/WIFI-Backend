package com.wificaptive.upload.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    @ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
    public MinioClient minioClient(MinioProperties properties) {
        String endpoint = properties.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            log.warn("MinIO is enabled but endpoint is blank. MinIO client will not be initialized.");
            return null;
        }

        log.info("Initializing MinIO Client for endpoint: {}, bucket: {}", endpoint, properties.getBucket());
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
