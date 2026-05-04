package com.memap.storage.dto.response;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;

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
}
