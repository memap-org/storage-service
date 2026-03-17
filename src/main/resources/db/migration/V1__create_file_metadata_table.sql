-- =====================================================
-- Storage Service - File Metadata Table
-- Version: V1
-- Description: Creates the file_metadata table for storing file information
-- =====================================================

CREATE TABLE IF NOT EXISTS file_metadata (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL COMMENT 'Stored filename (UUID-based)',
    original_name VARCHAR(500) NOT NULL COMMENT 'Original filename from upload',
    content_type VARCHAR(255) NOT NULL COMMENT 'MIME type of the file',
    size BIGINT NOT NULL COMMENT 'File size in bytes',
    md5_checksum VARCHAR(32) NOT NULL COMMENT 'MD5 hash for integrity verification',
    storage_path VARCHAR(500) NOT NULL COMMENT 'Relative path in local storage',
    owner_id VARCHAR(36) NOT NULL COMMENT 'User ID from JWT subject',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for querying files by owner (most common query pattern)
CREATE INDEX idx_file_metadata_owner_id ON file_metadata(owner_id);

-- Index for querying by creation date (for sorting)
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at DESC);
