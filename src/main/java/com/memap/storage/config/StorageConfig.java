package com.memap.storage.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StorageConfig {

  Local local = new Local();
  Minio minio = new Minio();
  String downloadUrlPrefix;

  @Getter
  @Setter
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Local {
    String baseDir = "/data/storage/files";
  }

  @Getter
  @Setter
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Minio {
    String endpoint;
    String accessKey;
    String secretKey;
    String bucketName;
  }
}
