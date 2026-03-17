# Change: Implement File Local Storage with MySQL Metadata

## Why

The storage-service currently has scaffolding (security, exception handling) but lacks core file storage functionality. Implementing local filesystem storage with MySQL metadata will:

1. **Enable File Operations**: Upload, download, and delete files for the MeMap platform
2. **Centralize File Management**: Single source of truth for file operations across all microservices
3. **Track File Metadata**: Store file information (name, size, content type, checksum, owner) in MySQL
4. **Support Multi-Tenancy**: Track file ownership via JWT subject for user-specific operations
5. **Provide Foundation**: Base implementation that can be extended to support cloud storage (MinIO/S3) in the future

## What Changes

### New Entities

- `FileMetadata` JPA entity for MySQL with fields: id, name, originalName, contentType, size, md5Checksum, storagePath, ownerId, createdAt, updatedAt

### New DTOs

- `FileUploadRequest` - Optional metadata for uploads (custom filename)
- `FileUploadResponse` - Response with file ID, download URL, metadata
- `FileInfoResponse` - File metadata response for queries

### New Services

- `IFileService` interface with upload/download/delete/getInfo methods
- `FileServiceImpl` implementation
- `IStorageBackend` interface for storage abstraction
- `LocalStorageBackend` implementation for local filesystem

### New Repository

- `FileMetadataRepository` Spring Data JPA repository

### New Controller

- `FileController` REST controller with endpoints:
  - `POST /file/upload` - Upload file with optional metadata
  - `GET /file/{fileId}/download` - Download file by ID
  - `GET /file/{fileId}` - Get file metadata
  - `GET /file/my-files` - List current user's files
  - `DELETE /file/{fileId}` - Delete file by ID

### Configuration

- Storage configuration properties: base directory path, max file size
- Database migration: Create `file_metadata` table

## Impact

- **Affected specs**: None (first implementation)
- **Affected code**:
  - `entity/FileMetadata.java` - NEW
  - `dto/FileUploadRequest.java` - NEW
  - `dto/FileUploadResponse.java` - NEW
  - `dto/FileInfoResponse.java` - NEW
  - `repository/FileMetadataRepository.java` - NEW
  - `service/IFileService.java` - NEW
  - `service/impl/FileServiceImpl.java` - NEW
  - `storage/IStorageBackend.java` - NEW
  - `storage/LocalStorageBackend.java` - NEW
  - `controller/FileController.java` - NEW
  - `config/StorageConfig.java` - NEW
  - `exception/ErrorCode.java` - ADD new error codes
  - `resources/application.yaml` - ADD storage configuration
  - `resources/db/migration/` - ADD schema migration

## API Endpoints Summary

| Method | Endpoint            | Description               | Auth Required |
| ------ | ------------------- | ------------------------- | ------------- |
| POST   | /file/upload        | Upload a file             | Yes           |
| GET    | /file/{id}/download | Download file by ID       | Yes           |
| GET    | /file/{id}          | Get file metadata         | Yes           |
| GET    | /file/my-files      | List current user's files | Yes           |
| DELETE | /file/{id}          | Delete file by ID         | Yes           |

## Risks and Mitigations

| Risk                             | Mitigation                                           |
| -------------------------------- | ---------------------------------------------------- |
| Disk space exhaustion            | Monitor storage usage; future: implement quotas      |
| Large file uploads blocking      | Configure appropriate timeouts; streaming support    |
| File path traversal attacks      | Validate/sanitize filenames; use UUID-based paths    |
| Orphaned files after failed DB   | Transactional approach; cleanup job (future)         |
| Permission issues on storage dir | Document required permissions; init check on startup |
