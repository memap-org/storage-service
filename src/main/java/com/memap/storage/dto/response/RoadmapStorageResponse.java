package com.memap.storage.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoadmapStorageResponse {
    String id;
    String roadmapId;
    String ownerId;
    Long usedStorage;
    Long maxStorage;
    Integer fileCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public String getUsedStorageFormatted() {
        return formatBytes(usedStorage != null ? usedStorage : 0L);
    }

    public String getMaxStorageFormatted() {
        return maxStorage != null ? formatBytes(maxStorage) : "—";
    }

    public double getUsedPercent() {
        if (maxStorage == null || maxStorage <= 0 || usedStorage == null) return 0.0;
        return Math.min(100.0, (double) usedStorage / maxStorage * 100);
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1_073_741_824L) return String.format("%.1f GB", bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576L) return String.format("%.1f MB", bytes / 1_048_576.0);
        if (bytes >= 1_024L) return String.format("%.1f KB", bytes / 1_024.0);
        return bytes + " B";
    }
}
