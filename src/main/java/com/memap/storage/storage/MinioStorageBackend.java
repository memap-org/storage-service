package com.memap.storage.storage;

import com.memap.storage.config.StorageConfig;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio", matchIfMissing = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MinioStorageBackend implements IStorageBackend {

  MinioClient minioClient;
  StorageConfig storageConfig;

  @Override
  @PostConstruct
  public void init() {
    String bucketName = storageConfig.getMinio().getBucketName();
    try {
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("Created MinIO bucket: {}", bucketName);
      } else {
        log.info("MinIO bucket already exists: {}", bucketName);
      }
    } catch (Exception e) {
      log.warn("Failed to initialize MinIO bucket '{}': {}", bucketName, e.getMessage());
    }
  }

  @Override
  public String store(MultipartFile file) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    String originalFilename = StringUtils.cleanPath(
        file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");

    LocalDate now = LocalDate.now();
    String year = String.valueOf(now.getYear());
    String month = String.format("%02d", now.getMonthValue());

    String extension = "";
    int dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex > 0) {
      extension = originalFilename.substring(dotIndex);
    }

    String objectKey = year + "/" + month + "/" + UUID.randomUUID() + extension;
    String bucketName = storageConfig.getMinio().getBucketName();

    try (InputStream inputStream = file.getInputStream()) {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucketName)
              .object(objectKey)
              .stream(inputStream, file.getSize(), -1)
              .contentType(file.getContentType())
              .build());
      log.debug("Stored file in MinIO: {}", objectKey);
      return objectKey;
    } catch (Exception e) {
      log.error("Failed to upload file to MinIO: {}", originalFilename, e);
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }
  }

  @Override
  public Resource load(String storagePath) {
    String bucketName = storageConfig.getMinio().getBucketName();
    try {
      InputStream inputStream = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucketName)
              .object(storagePath)
              .build());
      return new InputStreamResource(inputStream);
    } catch (ErrorResponseException e) {
      if ("NoSuchKey".equals(e.errorResponse().code())) {
        throw new AppException(ErrorCode.FILE_NOT_FOUND);
      }
      log.error("MinIO error loading file: {}", storagePath, e);
      throw new AppException(ErrorCode.FILE_NOT_FOUND);
    } catch (Exception e) {
      log.error("Failed to load file from MinIO: {}", storagePath, e);
      throw new AppException(ErrorCode.FILE_NOT_FOUND);
    }
  }

  @Override
  public boolean delete(String storagePath) {
    String bucketName = storageConfig.getMinio().getBucketName();
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(bucketName)
              .object(storagePath)
              .build());
      log.debug("Deleted file from MinIO: {}", storagePath);
      return true;
    } catch (ErrorResponseException e) {
      if ("NoSuchKey".equals(e.errorResponse().code())) {
        return false;
      }
      log.error("MinIO error deleting file: {}", storagePath, e);
      return false;
    } catch (Exception e) {
      log.error("Failed to delete file from MinIO: {}", storagePath, e);
      return false;
    }
  }
}
