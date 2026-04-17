package com.memap.storage.model;

import java.util.Arrays;
import java.util.Locale;

public enum RoadmapAssetType {
  ROADMAP_IMAGE,
  ROADMAP_RESOURCE,
  NODE_RESOURCE,
  NODE_INLINE_MEDIA;

  public static RoadmapAssetType fromValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("roadmapAssetType must not be blank");
    }

    String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(assetType -> assetType.name().equals(normalizedValue))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported roadmapAssetType: " + value));
  }

  public static RoadmapAssetType fromNullable(String value) {
    return value == null || value.isBlank() ? null : fromValue(value);
  }
}