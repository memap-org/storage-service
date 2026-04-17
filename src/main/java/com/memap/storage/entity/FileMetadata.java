package com.memap.storage.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "file_metadata")
@CompoundIndex(name = "idx_roadmapOwnerId_roadmapId", def = "{'roadmapOwnerId': 1, 'roadmapId': 1}")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMetadata {

  @Id
  String id;

  String name;

  String originalName;

  String contentType;

  Long size;

  String md5Checksum;

  String storagePath;

  @Indexed
  String ownerId;

  @Indexed
  String roadmapId;

  @Indexed
  String roadmapOwnerId;

  String roadmapAssetType;

  @CreatedDate
  LocalDateTime createdAt;

  @LastModifiedDate
  LocalDateTime updatedAt;
}
