package com.memap.storage.controller;

import com.memap.storage.dto.request.UpdateStorageQuotaRequest;
import com.memap.storage.dto.response.ApiResponse;
import com.memap.storage.service.RoadmapStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/roadmap-storage")
public class RoadmapStorageController {
    RoadmapStorageService roadmapStorageService;

    @PutMapping("/quota")
    public ApiResponse<Void> updateStorageQuota(@RequestBody UpdateStorageQuotaRequest request) {
        roadmapStorageService.updateStorageQuota(
                request.getMaxStorageCapacity(),
                request.getOwnerId()
        );
        return ApiResponse.<Void>builder()
                .message("Storage quota updated successfully")
                .build();
    }
}
