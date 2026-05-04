package com.memap.storage.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateStorageQuotaRequest {
    String ownerId;
    Long maxStorageCapacity;
}
