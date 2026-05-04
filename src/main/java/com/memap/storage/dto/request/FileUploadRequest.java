package com.memap.storage.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileUploadRequest {

  /**
   * Optional custom name for the file.
   * If not provided, the original filename will be used.
   */
  String customName;

  String roadmapId;

  String roadmapAssetType;
}
