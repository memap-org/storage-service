# Spec: MinIO Object Storage Backend

This spec defines the MinIO-based storage backend for file operations, replacing the local filesystem as the default backend.

## ADDED Requirements

### Requirement: MinIO Storage Backend

The system SHALL implement a MinIO object storage backend as the default storage implementation.

#### Scenario: Store file to MinIO bucket

- **GIVEN** a file upload request with valid content
- **WHEN** the system stores the file via `MinioStorageBackend`
- **THEN** the system SHALL upload the file to the configured MinIO bucket
- **AND** the object key SHALL follow the pattern `{year}/{month}/{uuid}.{ext}`
- **AND** the system SHALL return the object key as the storage path
- **AND** the system SHALL throw `FILE_UPLOAD_FAILED` if the MinIO operation fails

#### Scenario: Retrieve file from MinIO bucket

- **GIVEN** a valid object key (storage path)
- **WHEN** the system retrieves the file via `MinioStorageBackend.load()`
- **THEN** the system SHALL return the file content as an `InputStreamResource`
- **AND** the system SHALL throw `FILE_NOT_FOUND` if the object does not exist in the bucket

#### Scenario: Delete file from MinIO bucket

- **GIVEN** a valid object key (storage path)
- **WHEN** the system deletes the file via `MinioStorageBackend.delete()`
- **THEN** the system SHALL remove the object from the MinIO bucket
- **AND** the system SHALL return `true` if deletion was successful
- **AND** the system SHALL return `false` if the object did not exist

#### Scenario: Initialize MinIO bucket on startup

- **GIVEN** the application starts with `app.storage.backend=minio`
- **WHEN** `MinioStorageBackend.init()` runs
- **THEN** the system SHALL verify the configured bucket exists
- **AND** the system SHALL create the bucket if it does not exist
- **AND** the bucket SHALL be private (no public access policy)
- **AND** the system SHALL log the initialization result

### Requirement: MinIO Backend Conditional Activation

The system SHALL activate the MinIO backend only when explicitly configured as the active backend.

#### Scenario: MinIO backend active by default

- **GIVEN** `app.storage.backend` is not set or is set to `minio`
- **WHEN** the application context loads
- **THEN** `MinioStorageBackend` SHALL be the active `IStorageBackend` bean
- **AND** `LocalStorageBackend` SHALL NOT be instantiated

#### Scenario: Local backend can still be selected

- **GIVEN** `app.storage.backend=local` is set
- **WHEN** the application context loads
- **THEN** `LocalStorageBackend` SHALL be the active `IStorageBackend` bean
- **AND** `MinioStorageBackend` SHALL NOT be instantiated

### Requirement: MinIO Client Configuration

The system SHALL provide configurable MinIO connection properties.

#### Scenario: Configure MinIO connection via environment variables

- **GIVEN** environment variables `STORAGE_MINIO_ENDPOINT`, `STORAGE_MINIO_ACCESS_KEY`, `STORAGE_MINIO_SECRET_KEY`, `STORAGE_MINIO_BUCKET` are set
- **WHEN** the application starts
- **THEN** the `MinioClient` bean SHALL connect to the specified endpoint using the provided credentials
- **AND** the system SHALL use the specified bucket for all file operations

#### Scenario: Reject startup without MinIO credentials

- **GIVEN** `app.storage.backend=minio` and required MinIO properties are missing
- **WHEN** the application starts
- **THEN** the application SHALL fail to start with a meaningful configuration error

## MODIFIED Requirements

### Requirement: Storage Backend Abstraction (from `implement-file-local-storage/specs/file-local-storage`)

The `IStorageBackend` interface contract is unchanged; only the default implementation changes.

#### Scenario: Default backend is MinIO (previously local)

- **GIVEN** no explicit `app.storage.backend` is configured
- **WHEN** the application starts
- **THEN** the active storage backend SHALL be `MinioStorageBackend`
- **AND** the behaviour of `store()`, `load()`, and `delete()` SHALL remain identical from the caller's perspective (file paths as keys, `Resource` returned on load)
