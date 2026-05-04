package com.memap.storage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "File metadata information")
public class FileInfoResponse {

  @Schema(description = "Unique file identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
  String fileId;

  @Schema(description = "Display name (custom name if provided, otherwise original filename)", example = "my-document.pdf")
  String name;

  @Schema(description = "Original filename as uploaded", example = "document.pdf")
  String originalName;

  @Schema(description = "MIME content type", example = "application/pdf")
  String contentType;

  @Schema(description = "File size in bytes", example = "1048576")
  Long size;

  @Schema(description = "MD5 checksum for file integrity verification", example = "d41d8cd98f00b204e9800998ecf8427e")
  String md5Checksum;

  @Schema(description = "URL to download the file", example = "http://localhost:8088/storage/file/550e8400-e29b-41d4-a716-446655440000/download")
  String downloadUrl;

  @Schema(description = "Upload timestamp", example = "2026-02-06T10:30:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime createdAt;
}
