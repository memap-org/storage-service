package com.memap.storage.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memap.grpc.roadmap.RoadmapLookupServiceGrpc;
import com.memap.grpc.roadmap.ValidateRoadmapStorageContextRequest;
import com.memap.grpc.roadmap.ValidateRoadmapStorageContextResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoadmapGrpcClientImplTest {

  @Mock
  private RoadmapLookupServiceGrpc.RoadmapLookupServiceBlockingStub roadmapStub;

  private RoadmapGrpcClientImpl client;

  @BeforeEach
  void setUp() {
    client = new RoadmapGrpcClientImpl(5000L);
    client.setRoadmapStub(roadmapStub);
  }

  @Test
  void shouldReturnRpcResponseWhenValidationSucceeds() {
    ValidateRoadmapStorageContextResponse expectedResponse = ValidateRoadmapStorageContextResponse.newBuilder()
        .setRoadmapId("rm-1")
        .setUserId("user-1")
        .setFound(true)
        .setAllowed(true)
        .setRoadmapOwnerId("owner-1")
        .build();

    when(roadmapStub.withDeadlineAfter(5000L, TimeUnit.MILLISECONDS)).thenReturn(roadmapStub);
    when(roadmapStub.validateRoadmapStorageContext(any(ValidateRoadmapStorageContextRequest.class)))
        .thenReturn(expectedResponse);

    ValidateRoadmapStorageContextResponse response = client.validateRoadmapStorageContext("rm-1", "user-1");

    assertThat(response).isEqualTo(expectedResponse);
    verify(roadmapStub).withDeadlineAfter(5000L, TimeUnit.MILLISECONDS);
    verify(roadmapStub).validateRoadmapStorageContext(any(ValidateRoadmapStorageContextRequest.class));
  }

  @Test
  void shouldReturnFallbackWhenGrpcServiceIsUnavailable() {
    when(roadmapStub.withDeadlineAfter(5000L, TimeUnit.MILLISECONDS)).thenReturn(roadmapStub);
    when(roadmapStub.validateRoadmapStorageContext(any(ValidateRoadmapStorageContextRequest.class)))
        .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

    ValidateRoadmapStorageContextResponse response = client.validateRoadmapStorageContext("rm-1", "user-1");

    assertFallbackResponse(response, "rm-1", "user-1");
  }

  @Test
  void shouldReturnFallbackWhenDeadlineIsExceeded() {
    when(roadmapStub.withDeadlineAfter(5000L, TimeUnit.MILLISECONDS)).thenReturn(roadmapStub);
    when(roadmapStub.validateRoadmapStorageContext(any(ValidateRoadmapStorageContextRequest.class)))
        .thenThrow(new StatusRuntimeException(Status.DEADLINE_EXCEEDED));

    ValidateRoadmapStorageContextResponse response = client.validateRoadmapStorageContext("rm-2", "user-2");

    assertFallbackResponse(response, "rm-2", "user-2");
  }

  @Test
  void shouldReturnFallbackWhenUnexpectedExceptionOccurs() {
    when(roadmapStub.withDeadlineAfter(5000L, TimeUnit.MILLISECONDS)).thenReturn(roadmapStub);
    when(roadmapStub.validateRoadmapStorageContext(any(ValidateRoadmapStorageContextRequest.class)))
        .thenThrow(new IllegalStateException("boom"));

    ValidateRoadmapStorageContextResponse response = client.validateRoadmapStorageContext("rm-3", "user-3");

    assertFallbackResponse(response, "rm-3", "user-3");
  }

  @Test
  void shouldReturnFallbackWithoutCallingGrpcWhenInputIsBlank() {
    ValidateRoadmapStorageContextResponse response = client.validateRoadmapStorageContext("", "user-1");

    assertFallbackResponse(response, "", "user-1");
    verify(roadmapStub, never()).withDeadlineAfter(eq(5000L), eq(TimeUnit.MILLISECONDS));
  }

  private void assertFallbackResponse(
      ValidateRoadmapStorageContextResponse response,
      String roadmapId,
      String userId) {
    assertThat(response.getRoadmapId()).isEqualTo(roadmapId);
    assertThat(response.getUserId()).isEqualTo(userId);
    assertThat(response.getFound()).isFalse();
    assertThat(response.getAllowed()).isFalse();
    assertThat(response.getRoadmapOwnerId()).isEmpty();
  }
}