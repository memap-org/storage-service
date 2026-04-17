# Spec: File Metadata Storage

This spec defines the MongoDB-based metadata storage for files in the storage-service.

## MODIFIED Requirements

### Requirement: File Metadata Document

The system SHALL store file metadata in MongoDB database in a `file_metadata` collection.

#### Scenario: Store complete file metadata

- **GIVEN** a file upload is successful
- **WHEN** the system persists file metadata
- **THEN** the system SHALL store a document with the following fields:
  - `id`: Auto-generated UUID string (primary key / `_id` mapped)
  - `name`: Stored filename (UUID-based for uniqueness)
  - `originalName`: Original filename from upload
  - `contentType`: MIME type of the file
  - `size`: File size in bytes
  - `md5Checksum`: MD5 hash for integrity verification
  - `storagePath`: Relative path in local storage
  - `ownerId`: ID of the user who uploaded (from JWT subject)
  - `roadmapId`: Optional roadmap ID for roadmap-scoped uploads
  - `roadmapOwnerId`: Optional canonical roadmap owner ID
  - `roadmapAssetType`: Optional asset type (ROADMAP_IMAGE, ROADMAP_RESOURCE, etc.)
  - `createdAt`: Timestamp of upload (auto-populated via auditing)
  - `updatedAt`: Timestamp of last modification (auto-populated via auditing)

#### Scenario: Auto-generate UUID for file ID

- **GIVEN** a new file upload
- **WHEN** the metadata document is created
- **THEN** the system SHALL generate a UUID v4 for the `id` field
- **AND** the ID SHALL be unique across all documents

### Requirement: File Metadata Repository

The system SHALL provide a MongoDB repository for file metadata operations.

#### Scenario: Find file by ID

- **GIVEN** a valid file UUID
- **WHEN** querying `findById(id)`
- **THEN** the system SHALL return the file metadata if exists
- **OR** return empty Optional if not found

#### Scenario: Find files by owner

- **GIVEN** a valid owner ID
- **WHEN** querying `findByOwnerId(ownerId)`
- **THEN** the system SHALL return all file metadata documents for that owner

#### Scenario: Find files by owner ordered by creation date

- **GIVEN** a valid owner ID
- **WHEN** querying `findByOwnerIdOrderByCreatedAtDesc(ownerId)`
- **THEN** the system SHALL return all file metadata for that owner ordered by `createdAt` descending

#### Scenario: Delete file metadata by ID

- **GIVEN** a valid file UUID
- **WHEN** calling `deleteById(id)`
- **THEN** the system SHALL remove the metadata document from the collection

### Requirement: Roadmap Storage Aggregation Queries

The system SHALL provide aggregation queries for roadmap storage usage reporting.

#### Scenario: Get roadmap storage usage summary

- **GIVEN** a valid roadmap owner ID
- **WHEN** querying `getRoadmapStorageUsageSummary(roadmapOwnerId)`
- **THEN** the system SHALL aggregate across all documents where `roadmapOwnerId` matches and `roadmapId` is not null
- **AND** return `totalBytes` (sum of size), `totalFiles` (count), `roadmapCount` (distinct roadmapId count)
- **AND** return zeros when no matching documents exist

#### Scenario: Get per-roadmap storage usage items

- **GIVEN** a valid roadmap owner ID
- **WHEN** querying `findRoadmapStorageUsageItems(roadmapOwnerId)`
- **THEN** the system SHALL group documents by `roadmapId` where `roadmapOwnerId` matches and `roadmapId` is not null
- **AND** return per-group: `roadmapId`, `totalBytes` (sum of size), `fileCount`, `lastUploadedAt` (max createdAt)
- **AND** results SHALL be ordered by `totalBytes` descending, then `lastUploadedAt` descending

### Requirement: Collection Indexes

The system SHALL define indexes on the `file_metadata` collection for query performance.

#### Scenario: Index on ownerId

- **GIVEN** the `file_metadata` collection
- **THEN** the system SHALL maintain an index on `ownerId`

#### Scenario: Index on createdAt

- **GIVEN** the `file_metadata` collection
- **THEN** the system SHALL maintain a descending index on `createdAt`

#### Scenario: Index on roadmapId

- **GIVEN** the `file_metadata` collection
- **THEN** the system SHALL maintain an index on `roadmapId`

#### Scenario: Index on roadmapOwnerId

- **GIVEN** the `file_metadata` collection
- **THEN** the system SHALL maintain an index on `roadmapOwnerId`

#### Scenario: Compound index on roadmapOwnerId and roadmapId

- **GIVEN** the `file_metadata` collection
- **THEN** the system SHALL maintain a compound index on `{roadmapOwnerId, roadmapId}`

### Requirement: File Ownership Tracking

The system SHALL track file ownership for multi-tenant operations.

#### Scenario: Associate file with owner on upload

- **GIVEN** an authenticated user with JWT token
- **WHEN** uploading a file
- **THEN** the system SHALL extract user ID from JWT subject claim
- **AND** store it as `ownerId` in the file metadata document

#### Scenario: Query files by owner

- **GIVEN** an authenticated user
- **WHEN** listing their files
- **THEN** the system SHALL return only files where `ownerId` matches JWT subject

### Requirement: File Integrity Tracking

The system SHALL compute and store file checksums for integrity verification.

#### Scenario: Compute MD5 checksum on upload

- **GIVEN** a file upload
- **WHEN** processing the uploaded file
- **THEN** the system SHALL compute MD5 hash of file content
- **AND** store the hash in `md5Checksum` field (hex-encoded, 32 characters)

### Requirement: Auditing Timestamps

The system SHALL automatically manage document timestamps via Spring Data MongoDB auditing.

#### Scenario: Set createdAt on document creation

- **GIVEN** a new file metadata document
- **WHEN** the document is first saved
- **THEN** the system SHALL set `createdAt` to the current timestamp
- **AND** `createdAt` SHALL NOT change on subsequent updates

#### Scenario: Set updatedAt on document modification

- **GIVEN** an existing file metadata document
- **WHEN** the document is saved
- **THEN** the system SHALL set `updatedAt` to the current timestamp

## REMOVED Requirements

### Requirement: Flyway SQL Migrations

**Reason**: MongoDB is schema-flexible and does not require DDL migrations. Indexes are defined via annotations.
**Migration**: Remove `db/migration/` directory and Flyway dependencies.

### Requirement: JPA Entity Mapping

**Reason**: Replaced by Spring Data MongoDB `@Document` mapping.
**Migration**: Replace `@Entity`/`@Table`/`@Column` annotations with `@Document`/`@Field`/`@Indexed` annotations.
