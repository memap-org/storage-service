package com.memap.storage.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memap.grpc.roadmap.ValidateRoadmapStorageContextResponse;
import com.memap.storage.config.StorageConfig;
import com.memap.storage.dto.response.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import com.memap.storage.grpc.client.RoadmapGrpcClient;
import com.memap.storage.repository.FileMetadataRepository;
import com.memap.storage.service.RoadmapStorageService;
import com.memap.storage.storage.IStorageBackend;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class FileServiceImplRoadmapUploadTest {

  @Mock
  private FileMetadataRepository fileMetadataRepository;

  @Mock
  RoadmapStorageService roadmapStorageService;

  @Mock
  private IStorageBackend storageBackend;

  @Mock
  private StorageConfig storageConfig;

  @Mock
  private RoadmapGrpcClient roadmapGrpcClient;

  private FileServiceImpl fileService;

  private static final String USER_ID = "user-abc";
  private static final String ROADMAP_ID = "rm-001";
  private static final String ROADMAP_OWNER_ID = "owner-xyz";
  private static final String ASSET_TYPE = "ROADMAP_IMAGE";

  @BeforeEach
  void setUp() {
    fileService = new FileServiceImpl(fileMetadataRepository, storageBackend, storageConfig, roadmapGrpcClient, roadmapStorageService);
    setUpAuthentication(USER_ID);
  }

  // -------------------------------------------------------------------------
  // Happy-path: upload succeeds with gRPC validation
  // -------------------------------------------------------------------------

  @Test
  void uploadForRoadmap_shouldPersistRoadmapOwnerIdWhenValidationSucceeds() throws IOException {
    // Arrange
    MockMultipartFile file = buildFile("photo.png", "image/png", new byte[] { 1, 2, 3 });

    when(roadmapGrpcClient.validateRoadmapStorageContext(ROADMAP_ID, USER_ID))
        .thenReturn(allowedGrpcResponse(ROADMAP_ID, USER_ID, ROADMAP_OWNER_ID));
    when(storageBackend.store(file)).thenReturn("/data/storage/files/stored.png");
    when(storageConfig.getDownloadUrlPrefix()).thenReturn("http://localhost:8081");

    FileMetadata savedMetadata = savedEntity("file-id-1", "stored.png", "photo.png", ROADMAP_ID, ROADMAP_OWNER_ID,
        ASSET_TYPE);
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedMetadata);

    // Act
    FileUploadResponse response = fileService.uploadForRoadmap(file, null, ROADMAP_ID, ASSET_TYPE);

    // Assert – response is populated
    assertThat(response.getFileId()).isEqualTo("file-id-1");
    assertThat(response.getOriginalName()).isEqualTo("photo.png");

    // Assert – metadata saved with correct roadmap fields
    ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
    verify(fileMetadataRepository).save(captor.capture());
    FileMetadata persisted = captor.getValue();
    assertThat(persisted.getRoadmapId()).isEqualTo(ROADMAP_ID);
    assertThat(persisted.getRoadmapOwnerId()).isEqualTo(ROADMAP_OWNER_ID);
    assertThat(persisted.getRoadmapAssetType()).isEqualTo(ASSET_TYPE);
    assertThat(persisted.getOwnerId()).isEqualTo(USER_ID);
  }

  // -------------------------------------------------------------------------
  // Roadmap not found (gRPC found=false) → ROADMAP_NOT_FOUND
  // -------------------------------------------------------------------------

  @Test
  void uploadForRoadmap_shouldThrowRoadmapNotFoundWhenGrpcReturnsNotFound() throws IOException {
    MockMultipartFile file = buildFile("photo.png", "image/png", new byte[] { 1, 2, 3 });

    when(roadmapGrpcClient.validateRoadmapStorageContext(ROADMAP_ID, USER_ID))
        .thenReturn(notFoundGrpcResponse(ROADMAP_ID, USER_ID));

    assertThatThrownBy(() -> fileService.uploadForRoadmap(file, null, ROADMAP_ID, ASSET_TYPE))
        .isInstanceOf(AppException.class)
        .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ROADMAP_NOT_FOUND));

    verify(storageBackend, never()).store(any());
    verify(fileMetadataRepository, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // Forbidden (gRPC allowed=false) → ROADMAP_ACCESS_DENIED
  // -------------------------------------------------------------------------

  @Test
  void uploadForRoadmap_shouldThrowRoadmapAccessDeniedWhenGrpcReturnsNotAllowed() throws IOException {
    MockMultipartFile file = buildFile("photo.png", "image/png", new byte[] { 1, 2, 3 });

    when(roadmapGrpcClient.validateRoadmapStorageContext(ROADMAP_ID, USER_ID))
        .thenReturn(foundButDeniedGrpcResponse(ROADMAP_ID, USER_ID));

    assertThatThrownBy(() -> fileService.uploadForRoadmap(file, null, ROADMAP_ID, ASSET_TYPE))
        .isInstanceOf(AppException.class)
        .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
            .isEqualTo(ErrorCode.ROADMAP_ACCESS_DENIED));

    verify(storageBackend, never()).store(any());
    verify(fileMetadataRepository, never()).save(any());
  }

  // -------------------------------------------------------------------------
  // No roadmap context → gRPC must NOT be called
  // -------------------------------------------------------------------------

  @Test
  void upload_shouldNotCallGrpcWhenNoRoadmapIdProvided() throws IOException {
    MockMultipartFile file = buildFile("doc.pdf", "application/pdf", new byte[] { 1, 2, 3 });

    when(storageBackend.store(file)).thenReturn("/data/storage/files/doc.pdf");
    when(storageConfig.getDownloadUrlPrefix()).thenReturn("http://localhost:8081");

    FileMetadata saved = savedEntity("file-id-2", "doc.pdf", "doc.pdf", null, null, null);
    when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(saved);

    fileService.upload(file, null);

    verify(roadmapGrpcClient, never()).validateRoadmapStorageContext(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void setUpAuthentication(String userId) {
    Jwt jwt = Jwt.withTokenValue("test-token")
        .header("alg", "RS256")
        .claim("sub", userId)
        .build();
    var auth = new UsernamePasswordAuthenticationToken(jwt, null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  private MockMultipartFile buildFile(String originalName, String contentType, byte[] content) throws IOException {
    return new MockMultipartFile("file", originalName, contentType, new ByteArrayInputStream(content));
  }

  private ValidateRoadmapStorageContextResponse allowedGrpcResponse(String roadmapId, String userId, String ownerId) {
    return ValidateRoadmapStorageContextResponse.newBuilder()
        .setRoadmapId(roadmapId)
        .setUserId(userId)
        .setFound(true)
        .setAllowed(true)
        .setRoadmapOwnerId(ownerId)
        .build();
  }

  private ValidateRoadmapStorageContextResponse notFoundGrpcResponse(String roadmapId, String userId) {
    return ValidateRoadmapStorageContextResponse.newBuilder()
        .setRoadmapId(roadmapId)
        .setUserId(userId)
        .setFound(false)
        .setAllowed(false)
        .setRoadmapOwnerId("")
        .build();
  }

  private ValidateRoadmapStorageContextResponse foundButDeniedGrpcResponse(String roadmapId, String userId) {
    return ValidateRoadmapStorageContextResponse.newBuilder()
        .setRoadmapId(roadmapId)
        .setUserId(userId)
        .setFound(true)
        .setAllowed(false)
        .setRoadmapOwnerId("")
        .build();
  }

  private FileMetadata savedEntity(String id, String name, String originalName,
      String roadmapId, String roadmapOwnerId, String roadmapAssetType) {
    return FileMetadata.builder()
        .id(id)
        .name(name)
        .originalName(originalName)
        .contentType("image/png")
        .size(3L)
        .md5Checksum("5289df737df57326fcdd22597afb1fac")
        .storagePath("/data/storage/files/" + name)
        .ownerId(USER_ID)
        .roadmapId(roadmapId)
        .roadmapOwnerId(roadmapOwnerId)
        .roadmapAssetType(roadmapAssetType)
        .createdAt(java.time.LocalDateTime.now())
        .build();
  }
}
