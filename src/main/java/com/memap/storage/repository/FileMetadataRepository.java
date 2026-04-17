package com.memap.storage.repository;

import com.memap.storage.entity.FileMetadata;
import com.memap.storage.model.RoadmapStorageUsageItem;
import com.memap.storage.model.RoadmapStorageUsageSummary;
import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

  List<FileMetadata> findByOwnerId(String ownerId);

  List<FileMetadata> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

  @Aggregation(pipeline = {
      "{ $match: { roadmapOwnerId: ?0, roadmapId: { $ne: null } } }",
      "{ $group: { _id: null, totalBytes: { $sum: '$size' }, totalFiles: { $sum: 1 }, roadmapIds: { $addToSet: '$roadmapId' } } }",
      "{ $project: { _id: 0, totalBytes: 1, totalFiles: 1, roadmapCount: { $size: '$roadmapIds' } } }"
  })
  RoadmapStorageUsageSummary getRoadmapStorageUsageSummary(String roadmapOwnerId);

  @Aggregation(pipeline = {
      "{ $match: { roadmapOwnerId: ?0, roadmapId: { $ne: null } } }",
      "{ $group: { _id: '$roadmapId', totalBytes: { $sum: '$size' }, fileCount: { $sum: 1 }, lastUploadedAt: { $max: '$createdAt' } } }",
      "{ $project: { _id: 0, roadmapId: '$_id', totalBytes: 1, fileCount: 1, lastUploadedAt: 1 } }",
      "{ $sort: { totalBytes: -1, lastUploadedAt: -1 } }"
  })
  List<RoadmapStorageUsageItem> findRoadmapStorageUsageItems(String roadmapOwnerId);
}
