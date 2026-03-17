package com.memap.storage.service.impl;

import com.memap.storage.config.StorageConfig;
import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.dto.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import com.memap.storage.repository.FileMetadataRepository;
import com.memap.storage.service.IFileService;
import com.memap.storage.storage.IStorageBackend;
import com.memap.storage.util.ChecksumUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileServiceImpl implements IFileService {

  FileMetadataRepository fileMetadataRepository;
  IStorageBackend storageBackend;
  StorageConfig storageConfig;

  @Override
  @Transactional
  public FileUploadResponse upload(MultipartFile file, String customName) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    String currentUserId = getCurrentUserId();

    // Determine original name
    String originalName = StringUtils.hasText(customName)
        ? customName
        : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");

    // Compute MD5 checksum
    String md5Checksum;
    try {
      md5Checksum = ChecksumUtil.computeMd5(file.getInputStream());
    } catch (IOException e) {
      log.error("Failed to compute MD5 checksum", e);
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    // Store file to filesystem
    String storagePath = storageBackend.store(file);

    // Extract stored filename from path
    String storedFilename = storagePath.substring(storagePath.lastIndexOf('/') + 1);

    // Create metadata entity
    FileMetadata metadata = FileMetadata.builder()
        .name(storedFilename)
        .originalName(originalName)
        .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
        .size(file.getSize())
        .md5Checksum(md5Checksum)
        .storagePath(storagePath)
        .ownerId(currentUserId)
        .build();

    FileMetadata saved = fileMetadataRepository.save(metadata);

    String downloadUrl = buildDownloadUrl(saved.getId());

    return FileUploadResponse.builder()
        .fileId(saved.getId())
        .name(saved.getName())
        .originalName(saved.getOriginalName())
        .contentType(saved.getContentType())
        .size(saved.getSize())
        .md5Checksum(saved.getMd5Checksum())
        .downloadUrl(downloadUrl)
        .createdAt(saved.getCreatedAt())
        .build();
  }

  @Override
  public Resource download(String fileId) {
    FileMetadata metadata = getFileMetadata(fileId);
    return storageBackend.load(metadata.getStoragePath());
  }

  @Override
  public FileMetadata getFileMetadata(String fileId) {
    return fileMetadataRepository.findById(fileId)
        .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
  }

  @Override
  public FileInfoResponse getInfo(String fileId) {
    FileMetadata metadata = getFileMetadata(fileId);
    return mapToFileInfoResponse(metadata);
  }

  @Override
  public List<FileInfoResponse> getMyFiles() {
    String currentUserId = getCurrentUserId();
    List<FileMetadata> files = fileMetadataRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserId);
    return files.stream()
        .map(this::mapToFileInfoResponse)
        .toList();
  }

  @Override
  @Transactional
  public void delete(String fileId) {
    FileMetadata metadata = getFileMetadata(fileId);

    String currentUserId = getCurrentUserId();

    // Check ownership
    if (!metadata.getOwnerId().equals(currentUserId)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    // Delete from filesystem
    storageBackend.delete(metadata.getStoragePath());

    // Delete metadata from database
    fileMetadataRepository.delete(metadata);

    log.info("Deleted file: {} by user: {}", fileId, currentUserId);
  }

  private String getCurrentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (authentication.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }

    throw new AppException(ErrorCode.UNAUTHENTICATED);
  }

  private String buildDownloadUrl(String fileId) {
    String prefix = storageConfig.getDownloadUrlPrefix();
    if (prefix == null || prefix.isEmpty()) {
      prefix = "";
    }
    // Remove trailing slash if present
    if (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    return prefix + "/file/" + fileId + "/download";
  }

  private FileInfoResponse mapToFileInfoResponse(FileMetadata metadata) {
    return FileInfoResponse.builder()
        .fileId(metadata.getId())
        .name(metadata.getName())
        .originalName(metadata.getOriginalName())
        .contentType(metadata.getContentType())
        .size(metadata.getSize())
        .md5Checksum(metadata.getMd5Checksum())
        .downloadUrl(buildDownloadUrl(metadata.getId()))
        .createdAt(metadata.getCreatedAt())
        .build();
  }
}
