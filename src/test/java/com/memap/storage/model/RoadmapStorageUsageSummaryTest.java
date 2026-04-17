package com.memap.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoadmapStorageUsageSummaryTest {

  @Test
  void shouldNormalizeNullAggregateValuesToZero() {
    RoadmapStorageUsageSummary summary = new RoadmapStorageUsageSummary(null, null, null);

    assertThat(summary.getTotalBytes()).isZero();
    assertThat(summary.getTotalFiles()).isZero();
    assertThat(summary.getRoadmapCount()).isZero();
    assertThat(summary.hasUsage()).isFalse();
  }

  @Test
  void shouldCreateEmptySummary() {
    RoadmapStorageUsageSummary summary = RoadmapStorageUsageSummary.empty();

    assertThat(summary.getTotalBytes()).isZero();
    assertThat(summary.getTotalFiles()).isZero();
    assertThat(summary.getRoadmapCount()).isZero();
  }
}