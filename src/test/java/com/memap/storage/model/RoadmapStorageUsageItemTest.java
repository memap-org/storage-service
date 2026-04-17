package com.memap.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RoadmapStorageUsageItemTest {

  @Test
  void shouldNormalizeNullAggregateValuesToZero() {
    LocalDateTime uploadedAt = LocalDateTime.of(2026, 3, 23, 14, 30);

    RoadmapStorageUsageItem item = new RoadmapStorageUsageItem("rm-10", null, null, uploadedAt);

    assertThat(item.getRoadmapId()).isEqualTo("rm-10");
    assertThat(item.getTotalBytes()).isZero();
    assertThat(item.getFileCount()).isZero();
    assertThat(item.getLastUploadedAt()).isEqualTo(uploadedAt);
  }
}