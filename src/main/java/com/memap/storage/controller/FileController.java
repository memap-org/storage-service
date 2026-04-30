package com.memap.storage.controller;

import com.memap.storage.dto.ApiResponse;
import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.dto.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.model.RoadmapStorageUsageItem;
import com.memap.storage.model.RoadmapStorageUsageSummary;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @Parameter(description = "Custom display name for the file (optional)") @RequestParam(value = "customName", required = false) String customName,
            @Parameter(description = "Roadmap ID the file belongs to (optional)") @RequestParam(value = "roadmapId", required = false) String roadmapId,
            @Parameter(description = "Asset type within the roadmap (optional, e.g. ROADMAP_IMAGE, NODE_RESOURCE)") @RequestParam(value = "roadmapAssetType", required = false) String roadmapAssetType) {
        FileUploadResponse response = fileService.upload(file, customName, roadmapId, roadmapAssetType);
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
                        "attachment; filename=\"" + metadata.getOriginalName() + "\"; filename*=UTF-8''"
                                + encodedFilename)
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

    @PostMapping(value = "/upload-for-roadmap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file scoped to a roadmap", description = "Upload a file and associate it with a roadmap. The roadmap's existence and the caller's access are validated via gRPC before the file is stored. Maximum file size is 1024MB.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File uploaded successfully", content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File too large, invalid asset type, or missing parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller does not have access to upload to this roadmap"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Roadmap not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "File upload failed")
    })
    public ApiResponse<FileUploadResponse> uploadForRoadmap(
            @Parameter(description = "File to upload", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Custom display name for the file (optional)") @RequestParam(value = "customName", required = false) String customName,
            @Parameter(description = "Roadmap ID the file belongs to", required = true) @RequestParam("roadmapId") String roadmapId,
            @Parameter(description = "Asset type within the roadmap (e.g. THUMBNAIL, ATTACHMENT)", required = true) @RequestParam("roadmapAssetType") String roadmapAssetType) {
        FileUploadResponse response = fileService.uploadForRoadmap(file, customName, roadmapId, roadmapAssetType);
        return ApiResponse.<FileUploadResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/my-roadmap-storage/summary")
    @Operation(summary = "Get my roadmap storage usage summary", description = "Returns aggregate storage usage (total bytes, total files, roadmap count) across all roadmaps owned by the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully", content = @Content(schema = @Schema(implementation = RoadmapStorageUsageSummary.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ApiResponse<RoadmapStorageUsageSummary> getMyRoadmapStorageUsageSummary() {
        RoadmapStorageUsageSummary summary = fileService.getMyRoadmapStorageUsageSummary();
        return ApiResponse.<RoadmapStorageUsageSummary>builder()
                .result(summary)
                .build();
    }

    @GetMapping("/roadmap-storage/{roadmapOwnerId}/summary")
    @Operation(summary = "Get roadmap storage usage summary", description = "Returns aggregate storage usage (total bytes, total files, roadmap count) across all roadmaps owned by the specified user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully", content = @Content(schema = @Schema(implementation = RoadmapStorageUsageSummary.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ApiResponse<RoadmapStorageUsageSummary> getRoadmapStorageUsageSummary(
            @Parameter(description = "Roadmap owner user ID", required = true) @PathVariable String roadmapOwnerId) {
        RoadmapStorageUsageSummary summary = fileService.getRoadmapStorageUsageSummary(roadmapOwnerId);
        return ApiResponse.<RoadmapStorageUsageSummary>builder()
                .result(summary)
                .build();
    }

    @GetMapping("/my-roadmap-storage/items")
    @Operation(summary = "List my per-roadmap storage usage", description = "Returns a per-roadmap breakdown of storage usage (bytes, file count, last upload date) for all roadmaps owned by the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Items retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ApiResponse<List<RoadmapStorageUsageItem>> getMyRoadmapStorageUsageItems() {
        List<RoadmapStorageUsageItem> items = fileService.getMyRoadmapStorageUsageItems();
        return ApiResponse.<List<RoadmapStorageUsageItem>>builder()
                .result(items)
                .build();
    }

    @GetMapping("/roadmap-storage/{roadmapOwnerId}/items")
    @Operation(summary = "List per-roadmap storage usage", description = "Returns a per-roadmap breakdown of storage usage (bytes, file count, last upload date) for all roadmaps owned by the specified user, ordered by last-upload date descending.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Items retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ApiResponse<List<RoadmapStorageUsageItem>> getRoadmapStorageUsageItems(
            @Parameter(description = "Roadmap owner user ID", required = true) @PathVariable String roadmapOwnerId) {
        List<RoadmapStorageUsageItem> items = fileService.getRoadmapStorageUsageItems(roadmapOwnerId);
        return ApiResponse.<List<RoadmapStorageUsageItem>>builder()
                .result(items)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roadmap/{roadmapId}")
    public ApiResponse<Page<FileInfoResponse>> getStorageByRoadmapId(
            @PathVariable String roadmapId,
            @RequestParam(required = false) String search ,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {

        Page<FileInfoResponse> result = fileService.getStorageByRoadmapId(roadmapId, search, pageable);

        return ApiResponse.<Page<FileInfoResponse>>builder()
                .result(result)
                .build();
    }
}
