package com.memap.storage.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class RoadmapStorageUsageSummary {

  private final long totalBytes;
  private final long totalFiles;
  private final long roadmapCount;

  public RoadmapStorageUsageSummary(Long totalBytes, Long totalFiles, Long roadmapCount) {
    this.totalBytes = totalBytes != null ? totalBytes : 0L;
    this.totalFiles = totalFiles != null ? totalFiles : 0L;
    this.roadmapCount = roadmapCount != null ? roadmapCount : 0L;
  }

  public static RoadmapStorageUsageSummary empty() {
    return new RoadmapStorageUsageSummary(0L, 0L, 0L);
  }

  public boolean hasUsage() {
    return totalBytes > 0 || totalFiles > 0 || roadmapCount > 0;
  }
}