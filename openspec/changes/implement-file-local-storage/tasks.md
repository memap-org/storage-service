# Tasks: Implement File Local Storage with MySQL Metadata

## 1. Database Schema & Entity

- [x] 1.1 Create Flyway migration `V1__create_file_metadata_table.sql`
  - UUID primary key
  - Columns: name, original_name, content_type, size, md5_checksum, storage_path, owner_id
  - Timestamps: created_at, updated_at
  - Index on owner_id for user queries
- [x] 1.2 Create `FileMetadata` JPA entity
  - Use `@Entity`, `@Table(name = "file_metadata")`
  - UUID-based `@Id` with generation strategy
  - Proper Lombok annotations (@Builder, @Data, @FieldDefaults)
  - Audit timestamps with `@CreatedDate`, `@UpdatedDate`
- [x] 1.3 Create `FileMetadataRepository` interface
  - Extend `JpaRepository<FileMetadata, String>`
  - Add `findByOwnerId(String ownerId)` method
  - Add `findByOwnerIdOrderByCreatedAtDesc(String ownerId)` method

## 2. Storage Backend

- [x] 2.1 Create `IStorageBackend` interface
  - `String store(MultipartFile file)` - returns storage path
  - `Resource load(String storagePath)` - returns file resource
  - `boolean delete(String storagePath)`
  - `void init()` - initialize storage
- [x] 2.2 Create `StorageConfig` configuration class
  - Bind `app.storage.local.base-dir` property
  - Bind `app.storage.download-url-prefix` property
  - Create `LocalStorageBackend` bean
- [x] 2.3 Implement `LocalStorageBackend`
  - Initialize storage directory on startup
  - Generate date-based paths: `{year}/{month}/{uuid}.{ext}`
  - Store file using NIO Files API
  - Load file as `UrlResource`
  - Delete file with proper error handling
- [x] 2.4 Add MD5 checksum computation utility
  - Use `MessageDigest.getInstance("MD5")`
  - Return hex-encoded string (32 chars)

## 3. DTOs

- [x] 3.1 Create `FileUploadRequest` DTO
  - Optional `customName` field
  - Validation annotations
- [x] 3.2 Create `FileUploadResponse` DTO
  - `fileId`, `name`, `originalName`, `contentType`, `size`, `md5Checksum`, `downloadUrl`, `createdAt`
- [x] 3.3 Create `FileInfoResponse` DTO
  - Same fields as FileUploadResponse for consistency

## 4. Service Layer

- [x] 4.1 Create `IFileService` interface
  - `FileUploadResponse upload(MultipartFile file, String customName)`
  - `Resource download(String fileId)`
  - `FileInfoResponse getInfo(String fileId)`
  - `List<FileInfoResponse> getMyFiles()`
  - `void delete(String fileId)`
- [x] 4.2 Implement `FileServiceImpl`
  - Inject `FileMetadataRepository`, `IStorageBackend`, `StorageConfig`
  - Get current user ID from `SecurityContextHolder`
  - Compute MD5 checksum on upload
  - Generate download URL: `{prefix}/file/{fileId}/download`
  - Implement ownership check for delete operation
  - Map entity to DTO responses

## 5. Controller

- [x] 5.1 Create `FileController`
  - `@RestController`, `@RequestMapping("/file")`
  - `@Tag(name = "File")` for Swagger
- [x] 5.2 Implement `POST /upload` endpoint
  - Accept `@RequestParam("file") MultipartFile`
  - Optional `@RequestParam(required=false) String customName`
  - Return `ApiResponse<FileUploadResponse>`
- [x] 5.3 Implement `GET /{fileId}/download` endpoint
  - Return `ResponseEntity<Resource>` with proper headers
  - Content-Type, Content-Disposition, Content-Length
- [x] 5.4 Implement `GET /{fileId}` endpoint
  - Return `ApiResponse<FileInfoResponse>`
- [x] 5.5 Implement `GET /my-files` endpoint
  - Return `ApiResponse<List<FileInfoResponse>>`
- [x] 5.6 Implement `DELETE /{fileId}` endpoint
  - Return `ApiResponse<String>` with success message

## 6. Error Handling

- [x] 6.1 Add new error codes to `ErrorCode` enum (if needed)
  - Verify existing codes: FILE_NOT_FOUND (4200), FILE_UPLOAD_FAILED (4201), FILE_TOO_LARGE (4202)
  - Add STORAGE_ERROR (4205) for filesystem errors
- [ ] 6.2 Create `FileStorageException` for storage-specific errors
- [x] 6.3 Add exception handlers in `GlobalExceptionHandler`
  - Handle `MaxUploadSizeExceededException` → FILE_TOO_LARGE

## 7. Configuration

- [x] 7.1 Update `application.yaml` with storage properties
  - `app.storage.local.base-dir: /data/storage/files`
  - `app.storage.download-url-prefix: http://localhost:8088/storage`
- [x] 7.2 Configure Flyway for migrations
  - Add `spring.flyway.locations` if not present
  - Ensure `ddl-auto: none` is set (already present)

## 8. Security

- [x] 8.1 Update `SecurityConfig` to permit file endpoints for authenticated users
  - Ensure `/file/**` requires authentication
  - Verify JWT claims extraction works for owner_id

## 9. Testing

- [ ] 9.1 Write unit tests for `LocalStorageBackend`
  - Test store, load, delete operations
  - Test directory creation
- [ ] 9.2 Write unit tests for `FileServiceImpl`
  - Mock repository and storage backend
  - Test upload, download, delete flows
  - Test ownership validation
- [ ] 9.3 Write integration tests for `FileController`
  - Test all endpoints with mock authentication
  - Test error responses

## 10. Documentation

- [x] 10.1 Update README with file storage setup instructions
- [x] 10.2 Verify Swagger documentation is complete
  - Descriptions for all endpoints
  - Request/response examples

## Dependencies

```
1.* (Database & Entity) ─┐
2.* (Storage Backend) ───┼─► 4.* (Service) ─► 5.* (Controller)
3.* (DTOs) ──────────────┘
                                   │
6.* (Error Handling) ──────────────┴─► 5.* (Controller)
7.* (Configuration) ──────────────────► All
8.* (Security) ───────────────────────► 5.* (Controller)
9.* (Testing) ────────────────────────► After 1-8
10.* (Documentation) ─────────────────► After all
```
