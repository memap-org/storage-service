package com.memap.storage.storage;

import com.memap.storage.config.StorageConfig;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LocalStorageBackend implements IStorageBackend {

  StorageConfig storageConfig;

  @Override
  @PostConstruct
  public void init() {
    try {
      Path rootPath = Paths.get(storageConfig.getLocal().getBaseDir());
      if (!Files.exists(rootPath)) {
        Files.createDirectories(rootPath);
        log.info("Created storage directory: {}", rootPath.toAbsolutePath());
      }
      // Verify write permission
      if (!Files.isWritable(rootPath)) {
        throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
      }
      log.info("Storage backend initialized at: {}", rootPath.toAbsolutePath());
    } catch (IOException e) {
      log.error("Failed to initialize storage directory", e);
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }
  }

  @Override
  public String store(MultipartFile file) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null
        ? file.getOriginalFilename()
        : "unknown");

    // Generate date-based path: {year}/{month}/{uuid}.{ext}
    LocalDate now = LocalDate.now();
    String year = String.valueOf(now.getYear());
    String month = String.format("%02d", now.getMonthValue());

    String extension = "";
    int dotIndex = originalFilename.lastIndexOf('.');
    if (dotIndex > 0) {
      extension = originalFilename.substring(dotIndex);
    }

    String storedFilename = UUID.randomUUID().toString() + extension;
    String storagePath = year + "/" + month + "/" + storedFilename;

    try {
      Path basePath = Paths.get(storageConfig.getLocal().getBaseDir());
      Path targetDir = basePath.resolve(year).resolve(month);

      if (!Files.exists(targetDir)) {
        Files.createDirectories(targetDir);
      }

      Path targetPath = targetDir.resolve(storedFilename);
      Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

      log.debug("Stored file at: {}", storagePath);
      return storagePath;

    } catch (IOException e) {
      log.error("Failed to store file: {}", originalFilename, e);
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }
  }

  @Override
  public Resource load(String storagePath) {
    try {
      Path basePath = Paths.get(storageConfig.getLocal().getBaseDir());
      Path filePath = basePath.resolve(storagePath).normalize();

      // Security check: ensure path is within base directory
      if (!filePath.startsWith(basePath)) {
        throw new AppException(ErrorCode.FILE_NOT_FOUND);
      }

      Resource resource = new UrlResource(filePath.toUri());

      if (resource.exists() && resource.isReadable()) {
        return resource;
      } else {
        throw new AppException(ErrorCode.FILE_NOT_FOUND);
      }

    } catch (MalformedURLException e) {
      log.error("Failed to load file: {}", storagePath, e);
      throw new AppException(ErrorCode.FILE_NOT_FOUND);
    }
  }

  @Override
  public boolean delete(String storagePath) {
    try {
      Path basePath = Paths.get(storageConfig.getLocal().getBaseDir());
      Path filePath = basePath.resolve(storagePath).normalize();

      // Security check: ensure path is within base directory
      if (!filePath.startsWith(basePath)) {
        log.warn("Attempted to delete file outside storage directory: {}", storagePath);
        return false;
      }

      boolean deleted = Files.deleteIfExists(filePath);

      if (deleted) {
        log.debug("Deleted file: {}", storagePath);
      } else {
        log.debug("File not found for deletion: {}", storagePath);
      }

      return deleted;

    } catch (IOException e) {
      log.error("Failed to delete file: {}", storagePath, e);
      return false;
    }
  }
}
