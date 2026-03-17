package com.memap.storage.controller;

import com.memap.storage.dto.ApiResponse;
import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.dto.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.service.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "File", description = "File upload, download, and management APIs")
public class FileController {

  IFileService fileService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload a file", description = "Upload a file to storage with optional custom display name. Maximum file size is 1024MB.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File uploaded successfully", content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File too large or invalid request"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "File upload failed")
  })
  public ApiResponse<FileUploadResponse> upload(
      @Parameter(description = "File to upload", required = true) @RequestParam("file") MultipartFile file,
      @Parameter(description = "Custom display name for the file (optional)") @RequestParam(value = "customName", required = false) String customName) {
    FileUploadResponse response = fileService.upload(file, customName);
    return ApiResponse.<FileUploadResponse>builder()
        .result(response)
        .build();
  }

  @GetMapping("/{fileId}/download")
  @Operation(summary = "Download a file", description = "Download a file by its ID with Content-Disposition: attachment (forces download). Requires authentication.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File downloaded successfully", content = @Content(mediaType = "application/octet-stream")),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
  })
  public ResponseEntity<Resource> download(
      @Parameter(description = "File ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String fileId) {
    FileMetadata metadata = fileService.getFileMetadata(fileId);
    Resource resource = fileService.download(fileId);

    String encodedFilename = URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8)
        .replace("+", "%20");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + metadata.getOriginalName() + "\"; filename*=UTF-8''" + encodedFilename)
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadata.getSize()))
        .header("Content-MD5", metadata.getMd5Checksum())
        .body(resource);
  }

  @GetMapping("/{fileId}/access")
  @Operation(summary = "Access a file", description = "Access a file by its ID for inline viewing (images, PDFs display in browser). This endpoint is public and does not require authentication.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File accessed successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
  })
  public ResponseEntity<Resource> access(
      @Parameter(description = "File ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String fileId) {
    FileMetadata metadata = fileService.getFileMetadata(fileId);
    Resource resource = fileService.download(fileId);

    String encodedFilename = URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8)
        .replace("+", "%20");

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.getContentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "inline; filename=\"" + metadata.getOriginalName() + "\"; filename*=UTF-8''" + encodedFilename)
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metadata.getSize()))
        .header("Content-MD5", metadata.getMd5Checksum())
        .body(resource);
  }

  @GetMapping("/{fileId}")
  @Operation(summary = "Get file info", description = "Retrieve file metadata including name, size, content type, checksum, and download URL.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File info retrieved successfully", content = @Content(schema = @Schema(implementation = FileInfoResponse.class))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
  })
  public ApiResponse<FileInfoResponse> getInfo(
      @Parameter(description = "File ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String fileId) {
    FileInfoResponse response = fileService.getInfo(fileId);
    return ApiResponse.<FileInfoResponse>builder()
        .result(response)
        .build();
  }

  @GetMapping("/my-files")
  @Operation(summary = "List my files", description = "Get all files owned by the authenticated user, ordered by creation date (newest first).")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
  })
  public ApiResponse<List<FileInfoResponse>> getMyFiles() {
    List<FileInfoResponse> response = fileService.getMyFiles();
    return ApiResponse.<List<FileInfoResponse>>builder()
        .result(response)
        .build();
  }

  @DeleteMapping("/{fileId}")
  @Operation(summary = "Delete a file", description = "Delete a file by ID. Only the file owner can delete their files.")
  @ApiResponses(value = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File deleted successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - not the file owner"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
  })
  public ApiResponse<String> delete(
      @Parameter(description = "File ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable String fileId) {
    fileService.delete(fileId);
    return ApiResponse.<String>builder()
        .result("File deleted successfully")
        .build();
  }
}
