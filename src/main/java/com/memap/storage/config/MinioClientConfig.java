package com.memap.storage.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioClientConfig {

  private final StorageConfig storageConfig;

  @Bean
  @ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio", matchIfMissing = true)
  public MinioClient minioClient() {
    StorageConfig.Minio minio = storageConfig.getMinio();
    return MinioClient.builder()
        .endpoint(minio.getEndpoint())
        .credentials(minio.getAccessKey(), minio.getSecretKey())
        .build();
  }
}
