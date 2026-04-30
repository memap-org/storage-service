package com.memap.storage.service.impl;

import com.memap.grpc.roadmap.ValidateRoadmapStorageContextResponse;
import com.memap.storage.config.StorageConfig;
import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.dto.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import com.memap.storage.grpc.client.RoadmapGrpcClient;
import com.memap.storage.model.RoadmapStorageUsageItem;
import com.memap.storage.model.RoadmapStorageUsageSummary;
import com.memap.storage.repository.FileMetadataRepository;
import com.memap.storage.service.IFileService;
import com.memap.storage.storage.IStorageBackend;
import com.memap.storage.util.ChecksumUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
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
  RoadmapGrpcClient roadmapGrpcClient;

  @Override
  public FileUploadResponse upload(MultipartFile file, String customName) {
    return upload(file, customName, null, null);
  }

  @Override
  public FileUploadResponse upload(MultipartFile file, String customName, String roadmapId, String roadmapAssetType) {
    if (file.isEmpty()) {
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    String currentUserId = getCurrentUserId();
    String normalizedRoadmapId = StringUtils.hasText(roadmapId) ? roadmapId : null;
    String normalizedRoadmapAssetType = StringUtils.hasText(roadmapAssetType) ? roadmapAssetType : null;

    // Validate roadmap context via gRPC when a roadmapId is provided
    String roadmapOwnerId = null;
    if (normalizedRoadmapId != null) {
      ValidateRoadmapStorageContextResponse grpcResponse = roadmapGrpcClient
          .validateRoadmapStorageContext(normalizedRoadmapId, currentUserId);

      if (!grpcResponse.getFound()) {
        throw new AppException(ErrorCode.ROADMAP_NOT_FOUND);
      }
      if (!grpcResponse.getAllowed()) {
        throw new AppException(ErrorCode.ROADMAP_ACCESS_DENIED);
      }
      roadmapOwnerId = StringUtils.hasText(grpcResponse.getRoadmapOwnerId())
          ? grpcResponse.getRoadmapOwnerId()
          : null;
    }

    String originalName = StringUtils.hasText(customName)
        ? customName
        : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown");

    String md5Checksum;
    try {
      md5Checksum = ChecksumUtil.computeMd5(file.getInputStream());
    } catch (IOException e) {
      throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    String storagePath = storageBackend.store(file);
    String storedFilename = storagePath.substring(storagePath.lastIndexOf('/') + 1);

    FileMetadata metadata = new FileMetadata();
    metadata.setName(storedFilename);
    metadata.setOriginalName(originalName);
    metadata.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
    metadata.setSize(file.getSize());
    metadata.setMd5Checksum(md5Checksum);
    metadata.setStoragePath(storagePath);
    metadata.setOwnerId(currentUserId);
    metadata.setRoadmapId(normalizedRoadmapId);
    metadata.setRoadmapOwnerId(roadmapOwnerId);
    metadata.setRoadmapAssetType(normalizedRoadmapAssetType);

    FileMetadata saved = fileMetadataRepository.save(metadata);

    String downloadUrl = buildDownloadUrl(saved.getId());
    log.info("File uploaded: {} (original name: {}) by user: {}. Download URL: {}",
        saved.getId(), saved.getOriginalName(), currentUserId, downloadUrl);
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
  public FileUploadResponse uploadForRoadmap(MultipartFile file, String customName,
      String roadmapId, String roadmapAssetType) {
    return upload(file, customName, roadmapId, roadmapAssetType);
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

  @Override
  public RoadmapStorageUsageSummary getRoadmapStorageUsageSummary(String roadmapOwnerId) {
    RoadmapStorageUsageSummary summary = fileMetadataRepository.getRoadmapStorageUsageSummary(roadmapOwnerId);
    return summary != null ? summary : RoadmapStorageUsageSummary.empty();
  }

  @Override
  public List<RoadmapStorageUsageItem> getRoadmapStorageUsageItems(String roadmapOwnerId) {
    return fileMetadataRepository.findRoadmapStorageUsageItems(roadmapOwnerId);
  }

  @Override
  public RoadmapStorageUsageSummary getMyRoadmapStorageUsageSummary() {
    String currentUserId = getCurrentUserId();
    RoadmapStorageUsageSummary summary = fileMetadataRepository.getRoadmapStorageUsageSummary(currentUserId);
    return summary != null ? summary : RoadmapStorageUsageSummary.empty();
  }

  @Override
  public List<RoadmapStorageUsageItem> getMyRoadmapStorageUsageItems() {
    String currentUserId = getCurrentUserId();
    return fileMetadataRepository.findRoadmapStorageUsageItems(currentUserId);
  }

  @Override
  public Page<FileInfoResponse> getStorageByRoadmapId(String roadmapId, String search, Pageable pageable) {
      Page<FileMetadata> fileMetadata;
      if(search == null || search.trim().isEmpty() ) {
          fileMetadata = fileMetadataRepository.findByRoadmapId(roadmapId, pageable);
      } else
          fileMetadata = fileMetadataRepository.findByRoadmapIdAndOriginalNameContainingIgnoreCase(roadmapId, search, pageable);

    return fileMetadata.map(this::mapToFileInfoResponse);
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
