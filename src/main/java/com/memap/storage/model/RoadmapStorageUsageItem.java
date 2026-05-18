package com.memap.storage.model;

import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class RoadmapStorageUsageItem {

  private final String roadmapId;
  private final long totalBytes;
  private final long fileCount;
  private final LocalDateTime lastUploadedAt;

  @Setter
  private Long maxStorage;

  public RoadmapStorageUsageItem(String roadmapId, Long totalBytes, Long fileCount,
      LocalDateTime lastUploadedAt) {
    this.roadmapId = roadmapId;
    this.totalBytes = totalBytes != null ? totalBytes : 0L;
    this.fileCount = fileCount != null ? fileCount : 0L;
    this.lastUploadedAt = lastUploadedAt;
  }

  public String getUsedStorageFormatted() {
    return formatBytes(totalBytes);
  }

  public String getMaxStorageFormatted() {
    return maxStorage != null ? formatBytes(maxStorage) : "—";
  }

  public double getUsedPercent() {
    if (maxStorage == null || maxStorage <= 0) return 0.0;
    return Math.min(100.0, (double) totalBytes / maxStorage * 100);
  }

  private static String formatBytes(long bytes) {
    if (bytes >= 1_073_741_824L) return String.format("%.1f GB", bytes / 1_073_741_824.0);
    if (bytes >= 1_048_576L) return String.format("%.1f MB", bytes / 1_048_576.0);
    if (bytes >= 1_024L) return String.format("%.1f KB", bytes / 1_024.0);
    return bytes + " B";
  }
}