package com.memap.storage.grpc.client;

import com.memap.grpc.roadmap.ValidateRoadmapStorageContextResponse;

public interface RoadmapGrpcClient {

  ValidateRoadmapStorageContextResponse validateRoadmapStorageContext(String roadmapId, String userId);
}