package com.memap.storage.service.impl;

import com.memap.storage.dto.response.RoadmapStorageResponse;
import com.memap.storage.entity.RoadmapStorage;
import com.memap.storage.exception.AppException;
import com.memap.storage.exception.ErrorCode;
import com.memap.storage.repository.RoadmapStorageRepository;
import com.memap.storage.service.RoadmapStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class RoadmapStorageServiceImpl implements RoadmapStorageService {
    RoadmapStorageRepository roadmapStorageRepository;

    @Override
    public RoadmapStorage findByRoadmapId(String roadmapId) {
        return roadmapStorageRepository.findByRoadmapId(roadmapId)
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_STORAGE_NOT_FOUND));
    }

    @Override
    public List<RoadmapStorage> findAllByOwnerId(String ownerId) {
        return roadmapStorageRepository.findAllByOwnerId(ownerId);
    }

    @Override
    public void updateStorageQuota(Long maxStorageCapacity, String ownerId) {
        List<RoadmapStorage> storages = roadmapStorageRepository.findAllByOwnerId(ownerId);
        storages.forEach(storage -> storage.setMaxStorage(maxStorageCapacity));
        roadmapStorageRepository.saveAll(storages);
    }

    private RoadmapStorage createRoadmapStorage(String roadmapId, String ownerId) {
        RoadmapStorage storage = RoadmapStorage.builder()
                .ownerId(ownerId)
                .roadmapId(roadmapId)
                .maxStorage( 500L * 1024 * 1024) // 500MB
                .build();

        return roadmapStorageRepository.save(storage);
    }

    @Override
    public void uploadRoadmapFile(MultipartFile file, String roadmapId, String ownerId) {
        Long fileSize = file.getSize();

        RoadmapStorage storage = roadmapStorageRepository.findByRoadmapId(roadmapId)
                .orElseGet(() -> createRoadmapStorage(roadmapId, ownerId));

        long newUsedStorage = storage.getUsedStorage() + fileSize;

        if (newUsedStorage > storage.getMaxStorage()) {
            throw new AppException(ErrorCode.STORAGE_LIMIT_EXCEEDED);
        }

        storage.setUsedStorage(newUsedStorage);
        storage.setFileCount(storage.getFileCount() + 1);

        roadmapStorageRepository.save(storage);
    }

    @Override
    public void deleteRoadmapFile(Long fileSize, String roadmapId) {
        RoadmapStorage storage = roadmapStorageRepository.findByRoadmapId(roadmapId)
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_STORAGE_NOT_FOUND));

        Long previousStorage = storage.getUsedStorage();
        Integer previousFileCount = storage.getFileCount();

        Long updatedStorage = Math.max(0, previousStorage - fileSize);
        Integer updatedFileCount = Math.max(0, previousFileCount - 1);

        storage.setUsedStorage(updatedStorage);
        storage.setFileCount(updatedFileCount);

        log.info(
                "Deleted file from roadmap storage. roadmapId={}, fileSize={}, usedStorage={} -> {}, fileCount={} -> {}",
                roadmapId,
                fileSize,
                previousStorage,
                updatedStorage,
                previousFileCount,
                updatedFileCount
        );

        roadmapStorageRepository.save(storage);
    }
}
