package com.memap.storage.service;

import com.memap.storage.entity.RoadmapStorage;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface RoadmapStorageService {
    RoadmapStorage findByRoadmapId(String roadmapId);
    void updateStorageQuota(Long maxStorageCapacity, String ownerId);
    void uploadRoadmapFile(MultipartFile file, String roadmapId, String ownerId);
    void deleteRoadmapFile(Long fileSize, String roadmapId);
}
