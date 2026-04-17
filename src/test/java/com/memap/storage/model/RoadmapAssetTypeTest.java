package com.memap.storage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoadmapAssetTypeTest {

  @Test
  void shouldParseCaseInsensitiveValues() {
    assertThat(RoadmapAssetType.fromValue(" roadmap_image "))
        .isEqualTo(RoadmapAssetType.ROADMAP_IMAGE);
  }

  @Test
  void shouldReturnNullForBlankNullableValue() {
    assertThat(RoadmapAssetType.fromNullable("  ")).isNull();
  }

  @Test
  void shouldRejectUnsupportedValue() {
    assertThatThrownBy(() -> RoadmapAssetType.fromValue("avatar"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported roadmapAssetType");
  }
}