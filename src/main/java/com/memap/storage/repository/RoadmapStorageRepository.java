package com.memap.storage.repository;

import com.memap.storage.entity.RoadmapStorage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadmapStorageRepository extends MongoRepository<RoadmapStorage, String> {
    Optional<RoadmapStorage> findByRoadmapId(String roadmapId);
    List<RoadmapStorage> findAllByOwnerId(String ownerId);
}
