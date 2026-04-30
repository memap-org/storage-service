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
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageBackendTest {

  @Mock
  private MinioClient minioClient;

  @Mock
  private StorageConfig storageConfig;

  @InjectMocks
  private MinioStorageBackend backend;

  private StorageConfig.Minio minioProps;

  @BeforeEach
  void setUp() {
    minioProps = new StorageConfig.Minio();
    minioProps.setBucketName("test-bucket");
    minioProps.setEndpoint("http://localhost:9000");
    minioProps.setAccessKey("minioadmin");
    minioProps.setSecretKey("minioadmin");
    when(storageConfig.getMinio()).thenReturn(minioProps);
  }

  // ── init() ──────────────────────────────────────────────────────────────

  @Test
  void init_createsBucket_whenBucketDoesNotExist() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    backend.init();

    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void init_doesNotCreateBucket_whenBucketAlreadyExists() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    backend.init();

    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void init_doesNotThrow_whenMinioIsUnreachable() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("connection refused"));

    // should not propagate
    backend.init();
  }

  // ── store() ─────────────────────────────────────────────────────────────

  @Test
  void store_uploadsFileAndReturnsObjectKey() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "photo.jpg", "image/jpeg", "data".getBytes());

    String path = backend.store(file);

    verify(minioClient).putObject(any(PutObjectArgs.class));
    assertThat(path).matches("\\d{4}/\\d{2}/[a-f0-9\\-]+\\.jpg");
  }

  @Test
  void store_throwsFileUploadFailed_whenFileIsEmpty() {
    MultipartFile empty = new MockMultipartFile("file", new byte[0]);

    assertThatThrownBy(() -> backend.store(empty))
        .isInstanceOf(AppException.class)
        .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
  }

  @Test
  void store_throwsFileUploadFailed_whenMinioThrows() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file", "doc.pdf", "application/pdf", "data".getBytes());
    doThrow(new RuntimeException("minio error")).when(minioClient).putObject(any(PutObjectArgs.class));

    assertThatThrownBy(() -> backend.store(file))
        .isInstanceOf(AppException.class)
        .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
  }

  // ── load() ──────────────────────────────────────────────────────────────

  @Test
  void load_returnsResource_whenObjectExists() throws Exception {
    when(minioClient.getObject(any(GetObjectArgs.class)))
        .thenReturn(null); // MinioClient.getObject returns GetObjectResponse (InputStream subtype)
    // Use a real stream to avoid NPE in InputStreamResource
    ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
    when(minioClient.getObject(any(GetObjectArgs.class))).thenAnswer(inv -> stream);

    Resource resource = backend.load("2024/01/file.jpg");

    assertThat(resource).isNotNull();
  }

  @Test
  void load_throwsFileNotFound_whenObjectDoesNotExist() throws Exception {
    ErrorResponse errorResponse = mock(ErrorResponse.class);
    when(errorResponse.code()).thenReturn("NoSuchKey");
    ErrorResponseException ex = new ErrorResponseException(errorResponse, null, null);
    when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(ex);

    assertThatThrownBy(() -> backend.load("2024/01/missing.jpg"))
        .isInstanceOf(AppException.class)
        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
  }

  // ── delete() ────────────────────────────────────────────────────────────

  @Test
  void delete_returnsTrueOnSuccess() throws Exception {
    doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

    boolean result = backend.delete("2024/01/file.jpg");

    assertThat(result).isTrue();
  }

  @Test
  void delete_returnsFalse_whenObjectDoesNotExist() throws Exception {
    ErrorResponse errorResponse = mock(ErrorResponse.class);
    when(errorResponse.code()).thenReturn("NoSuchKey");
    ErrorResponseException ex = new ErrorResponseException(errorResponse, null, null);
    doThrow(ex).when(minioClient).removeObject(any(RemoveObjectArgs.class));

    boolean result = backend.delete("2024/01/missing.jpg");

    assertThat(result).isFalse();
  }
}
