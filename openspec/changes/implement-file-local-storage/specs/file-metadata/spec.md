# Spec: File Metadata Storage

This spec defines the MySQL-based metadata storage for files in the storage-service.

## ADDED Requirements

### Requirement: File Metadata Entity

The system SHALL store file metadata in MySQL database with a `file_metadata` table.

#### Scenario: Store complete file metadata

- **GIVEN** a file upload is successful
- **WHEN** the system persists file metadata
- **THEN** the system SHALL store the following fields:
  - `id`: Auto-generated UUID (primary key)
  - `name`: Stored filename (UUID-based for uniqueness)
  - `original_name`: Original filename from upload
  - `content_type`: MIME type of the file
  - `size`: File size in bytes
  - `md5_checksum`: MD5 hash for integrity verification
  - `storage_path`: Relative path in local storage
  - `owner_id`: ID of the user who uploaded (from JWT subject)
  - `created_at`: Timestamp of upload
  - `updated_at`: Timestamp of last modification

#### Scenario: Auto-generate UUID for file ID

- **GIVEN** a new file upload
- **WHEN** the metadata record is created
- **THEN** the system SHALL generate a UUID v4 for the `id` field
- **AND** the ID SHALL be unique across all records

### Requirement: File Metadata Repository

The system SHALL provide a JPA repository for file metadata operations.

#### Scenario: Find file by ID

- **GIVEN** a valid file UUID
- **WHEN** querying `findById(id)`
- **THEN** the system SHALL return the file metadata if exists
- **OR** return empty Optional if not found

#### Scenario: Find files by owner

- **GIVEN** a valid owner ID
- **WHEN** querying `findByOwnerId(ownerId)`
- **THEN** the system SHALL return all file metadata records for that owner
- **AND** results SHALL be ordered by `created_at` descending

#### Scenario: Check file exists by ID

- **GIVEN** a file UUID
- **WHEN** querying `existsById(id)`
- **THEN** the system SHALL return true if file metadata exists
- **OR** return false if not found

#### Scenario: Delete file metadata by ID

- **GIVEN** a valid file UUID
- **WHEN** calling `deleteById(id)`
- **THEN** the system SHALL remove the metadata record from database

### Requirement: File Ownership Tracking

The system SHALL track file ownership for multi-tenant operations.

#### Scenario: Associate file with owner on upload

- **GIVEN** an authenticated user with JWT token
- **WHEN** uploading a file
- **THEN** the system SHALL extract user ID from JWT subject claim
- **AND** store it as `owner_id` in file metadata

#### Scenario: Query files by owner

- **GIVEN** an authenticated user
- **WHEN** listing their files
- **THEN** the system SHALL return only files where `owner_id` matches JWT subject

### Requirement: File Integrity Tracking

The system SHALL compute and store file checksums for integrity verification.

#### Scenario: Compute MD5 checksum on upload

- **GIVEN** a file upload
- **WHEN** processing the uploaded file
- **THEN** the system SHALL compute MD5 hash of file content
- **AND** store the hash in `md5_checksum` field (hex-encoded, 32 characters)

#### Scenario: Verify file integrity on download

- **GIVEN** a file download request
- **WHEN** serving the file
- **THEN** the system SHALL include `Content-MD5` header with stored checksum
