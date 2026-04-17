package com.memap.storage.model;

import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class RoadmapStorageUsageItem {

  private final String roadmapId;
  private final long totalBytes;
  private final long fileCount;
  private final LocalDateTime lastUploadedAt;

  public RoadmapStorageUsageItem(String roadmapId, Long totalBytes, Long fileCount,
      LocalDateTime lastUploadedAt) {
    this.roadmapId = roadmapId;
    this.totalBytes = totalBytes != null ? totalBytes : 0L;
    this.fileCount = fileCount != null ? fileCount : 0L;
    this.lastUploadedAt = lastUploadedAt;
  }
}