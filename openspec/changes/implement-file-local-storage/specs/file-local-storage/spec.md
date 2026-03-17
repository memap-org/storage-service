# Spec: Local File Storage

This spec defines the local filesystem storage backend and REST API for file operations.

## ADDED Requirements

### Requirement: Local Storage Backend

The system SHALL implement a local filesystem storage backend for file operations.

#### Scenario: Store file to local filesystem

- **GIVEN** a file upload request with valid content
- **WHEN** the system stores the file
- **THEN** the system SHALL create the file at `{base-dir}/{year}/{month}/{uuid}.{ext}`
- **AND** the system SHALL create parent directories if they don't exist
- **AND** the system SHALL return the relative storage path

#### Scenario: Retrieve file from local filesystem

- **GIVEN** a valid storage path
- **WHEN** the system retrieves the file
- **THEN** the system SHALL return the file content as byte stream
- **AND** the system SHALL throw `FILE_NOT_FOUND` error if file doesn't exist

#### Scenario: Delete file from local filesystem

- **GIVEN** a valid storage path
- **WHEN** the system deletes the file
- **THEN** the system SHALL remove the file from filesystem
- **AND** the system SHALL return true if deletion successful
- **AND** the system SHALL return false if file didn't exist

#### Scenario: Verify storage directory on startup

- **GIVEN** the application starts
- **WHEN** initializing the storage backend
- **THEN** the system SHALL verify the base directory exists
- **AND** the system SHALL create it if it doesn't exist
- **AND** the system SHALL verify write permissions

### Requirement: Storage Path Organization

The system SHALL organize files in a date-based directory structure.

#### Scenario: Generate storage path for new file

- **GIVEN** a new file upload with original filename "document.pdf"
- **WHEN** generating storage path
- **THEN** the system SHALL create path like `2026/02/{uuid}.pdf`
- **WHERE** year and month are from current date
- **AND** uuid is a unique identifier

#### Scenario: Handle files without extension

- **GIVEN** a file upload with no extension in filename
- **WHEN** generating storage path
- **THEN** the system SHALL create path like `2026/02/{uuid}` without extension

### Requirement: File Upload API

The system SHALL provide REST endpoint for file uploads.

#### Scenario: Upload file successfully

- **GIVEN** an authenticated user
- **WHEN** sending `POST /file/upload` with multipart/form-data containing file
- **THEN** the system SHALL store the file in local storage
- **AND** the system SHALL create metadata record in MySQL
- **AND** the system SHALL return 200 OK with FileUploadResponse
- **AND** the response SHALL include `fileId`, `downloadUrl`, `name`, `size`, `contentType`

#### Scenario: Upload file with custom filename

- **GIVEN** an authenticated user
- **WHEN** sending `POST /file/upload` with file and `customName` parameter
- **THEN** the system SHALL use custom name for `original_name` field
- **AND** the system SHALL still generate unique storage filename

#### Scenario: Upload file exceeding size limit

- **GIVEN** an authenticated user
- **WHEN** uploading a file larger than configured max size (1024MB)
- **THEN** the system SHALL return 400 Bad Request
- **AND** the error code SHALL be `FILE_TOO_LARGE` (4202)

#### Scenario: Upload with no file provided

- **GIVEN** an authenticated user
- **WHEN** sending `POST /file/upload` without file part
- **THEN** the system SHALL return 400 Bad Request

#### Scenario: Upload without authentication

- **GIVEN** a request without valid JWT token
- **WHEN** sending `POST /file/upload`
- **THEN** the system SHALL return 401 Unauthorized

### Requirement: File Download API

The system SHALL provide REST endpoint for file downloads.

#### Scenario: Download file successfully

- **GIVEN** an authenticated user
- **AND** a valid file ID
- **WHEN** sending `GET /file/{fileId}/download`
- **THEN** the system SHALL return 200 OK with file content
- **AND** the response SHALL have `Content-Type` header matching stored MIME type
- **AND** the response SHALL have `Content-Disposition: attachment; filename="{original_name}"`
- **AND** the response SHALL have `Content-Length` header

#### Scenario: Download non-existent file

- **GIVEN** an authenticated user
- **WHEN** sending `GET /file/{fileId}/download` with invalid ID
- **THEN** the system SHALL return 404 Not Found
- **AND** the error code SHALL be `FILE_NOT_FOUND` (4200)

#### Scenario: Download without authentication

- **GIVEN** a request without valid JWT token
- **WHEN** sending `GET /file/{fileId}/download`
- **THEN** the system SHALL return 401 Unauthorized

### Requirement: File Metadata API

The system SHALL provide REST endpoint for retrieving file metadata.

#### Scenario: Get file metadata successfully

- **GIVEN** an authenticated user
- **AND** a valid file ID
- **WHEN** sending `GET /file/{fileId}`
- **THEN** the system SHALL return 200 OK with FileInfoResponse
- **AND** the response SHALL include `id`, `name`, `originalName`, `contentType`, `size`, `md5Checksum`, `downloadUrl`, `createdAt`

#### Scenario: Get metadata for non-existent file

- **GIVEN** an authenticated user
- **WHEN** sending `GET /file/{fileId}` with invalid ID
- **THEN** the system SHALL return 404 Not Found

### Requirement: List User Files API

The system SHALL provide REST endpoint for listing user's files.

#### Scenario: List current user's files

- **GIVEN** an authenticated user with files
- **WHEN** sending `GET /file/my-files`
- **THEN** the system SHALL return 200 OK with list of FileInfoResponse
- **AND** the list SHALL contain only files owned by the current user
- **AND** the list SHALL be ordered by creation date descending

#### Scenario: List files when user has none

- **GIVEN** an authenticated user with no files
- **WHEN** sending `GET /file/my-files`
- **THEN** the system SHALL return 200 OK with empty list

### Requirement: File Delete API

The system SHALL provide REST endpoint for file deletion.

#### Scenario: Delete file successfully

- **GIVEN** an authenticated user
- **AND** a file owned by the user
- **WHEN** sending `DELETE /file/{fileId}`
- **THEN** the system SHALL delete file from local storage
- **AND** the system SHALL delete metadata from MySQL
- **AND** the system SHALL return 200 OK with success message

#### Scenario: Delete non-existent file

- **GIVEN** an authenticated user
- **WHEN** sending `DELETE /file/{fileId}` with invalid ID
- **THEN** the system SHALL return 404 Not Found

#### Scenario: Delete file owned by another user

- **GIVEN** an authenticated user
- **AND** a file owned by different user
- **WHEN** sending `DELETE /file/{fileId}`
- **THEN** the system SHALL return 403 Forbidden
- **AND** the error code SHALL be `FORBIDDEN` (4104)

### Requirement: Storage Configuration

The system SHALL support configurable storage settings.

#### Scenario: Configure base storage directory

- **GIVEN** application property `app.storage.local.base-dir=/data/storage/files`
- **WHEN** the application starts
- **THEN** the system SHALL use the specified directory for file storage

#### Scenario: Configure download URL prefix

- **GIVEN** application property `app.storage.download-url-prefix=http://localhost:8088/storage`
- **WHEN** generating download URLs
- **THEN** the system SHALL use the prefix: `{prefix}/file/{fileId}/download`

### Requirement: API Response Wrapper

The system SHALL use consistent API response format.

#### Scenario: Successful response format

- **GIVEN** a successful API operation
- **WHEN** returning response
- **THEN** the response SHALL have structure: `{"code": 0, "message": "Success", "result": {...}}`

#### Scenario: Error response format

- **GIVEN** an error during API operation
- **WHEN** returning error response
- **THEN** the response SHALL have structure: `{"code": <error_code>, "message": "<error_message>", "result": null}`
