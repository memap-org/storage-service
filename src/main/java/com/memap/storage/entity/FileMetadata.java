package com.memap.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_metadata_owner_id", columnList = "ownerId"),
    @Index(name = "idx_file_metadata_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  String id;

  @Column(nullable = false)
  String name;

  @Column(name = "original_name", nullable = false, length = 500)
  String originalName;

  @Column(name = "content_type", nullable = false)
  String contentType;

  @Column(nullable = false)
  Long size;

  @Column(name = "md5_checksum", nullable = false, length = 32)
  String md5Checksum;

  @Column(name = "storage_path", nullable = false, length = 500)
  String storagePath;

  @Column(name = "owner_id", nullable = false)
  String ownerId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  LocalDateTime updatedAt;
}
