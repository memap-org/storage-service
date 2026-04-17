package com.memap.storage.grpc.client;

import com.memap.grpc.roadmap.RoadmapLookupServiceGrpc;
import com.memap.grpc.roadmap.ValidateRoadmapStorageContextRequest;
import com.memap.grpc.roadmap.ValidateRoadmapStorageContextResponse;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoadmapGrpcClientImpl implements RoadmapGrpcClient {

  private final long deadlineMs;

  private RoadmapLookupServiceGrpc.RoadmapLookupServiceBlockingStub roadmapStub;

  public RoadmapGrpcClientImpl(@Value("${grpc.client.roadmap-service.deadline:5000}") long deadlineMs) {
    this.deadlineMs = deadlineMs;
  }

  @GrpcClient("roadmap-service")
  void setRoadmapStub(RoadmapLookupServiceGrpc.RoadmapLookupServiceBlockingStub roadmapStub) {
    this.roadmapStub = roadmapStub;
  }

  @Override
  public ValidateRoadmapStorageContextResponse validateRoadmapStorageContext(String roadmapId, String userId) {
    log.debug("gRPC validateRoadmapStorageContext called for roadmapId={} userId={}", roadmapId, userId);

    if (roadmapId == null || roadmapId.isBlank() || userId == null || userId.isBlank()) {
      return buildFallbackResponse(roadmapId, userId);
    }

    try {
      ValidateRoadmapStorageContextRequest request = ValidateRoadmapStorageContextRequest.newBuilder()
          .setRoadmapId(roadmapId)
          .setUserId(userId)
          .build();

      ValidateRoadmapStorageContextResponse response = roadmapStub
          .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
          .validateRoadmapStorageContext(request);

      log.debug(
          "gRPC validateRoadmapStorageContext response for roadmapId={} userId={}: found={} allowed={}",
          roadmapId,
          userId,
          response.getFound(),
          response.getAllowed());
      return response;
    } catch (StatusRuntimeException exception) {
      log.warn(
          "gRPC call failed for validateRoadmapStorageContext roadmapId={} userId={}: {} - {}",
          roadmapId,
          userId,
          exception.getStatus().getCode(),
          exception.getStatus().getDescription());
      return buildFallbackResponse(roadmapId, userId);
    } catch (Exception exception) {
      log.error(
          "Unexpected error in validateRoadmapStorageContext for roadmapId={} userId={}: {}",
          roadmapId,
          userId,
          exception.getMessage(),
          exception);
      return buildFallbackResponse(roadmapId, userId);
    }
  }

  private ValidateRoadmapStorageContextResponse buildFallbackResponse(String roadmapId, String userId) {
    return ValidateRoadmapStorageContextResponse.newBuilder()
        .setRoadmapId(roadmapId != null ? roadmapId : "")
        .setUserId(userId != null ? userId : "")
        .setFound(false)
        .setAllowed(false)
        .setRoadmapOwnerId("")
        .build();
  }
}