package com.memap.storage.grpc.server;

import com.memap.grpc.storage.FileInfoData;
import com.memap.grpc.storage.FileLookupServiceGrpc;
import com.memap.grpc.storage.GetFileInfoRequest;
import com.memap.grpc.storage.GetFileInfoResponse;
import com.memap.grpc.storage.GetFileInfosRequest;
import com.memap.grpc.storage.GetFileInfosResponse;
import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.exception.AppException;
import com.memap.storage.service.IFileService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FileGrpcService extends FileLookupServiceGrpc.FileLookupServiceImplBase {

  private final IFileService fileService;

  @Override
  public void getFileInfo(GetFileInfoRequest request, StreamObserver<GetFileInfoResponse> responseObserver) {
    String fileId = request.getFileId();
    log.debug("gRPC getFileInfo called for fileId: {}", fileId);

    try {
      GetFileInfoResponse.Builder responseBuilder = GetFileInfoResponse.newBuilder()
          .setFileId(fileId != null ? fileId : "");

      if (!StringUtils.hasText(fileId)) {
        responseBuilder.setFound(false);
      } else {
        FileInfoResponse fileInfo = fileService.getInfo(fileId);
        responseBuilder
            .setFound(true)
            .setName(defaultString(fileInfo.getName()))
            .setOriginalName(defaultString(fileInfo.getOriginalName()))
            .setContentType(defaultString(fileInfo.getContentType()))
            .setSize(fileInfo.getSize() != null ? fileInfo.getSize() : 0L)
            .setDownloadUrl(defaultString(fileInfo.getDownloadUrl()))
            .setCreatedAt(fileInfo.getCreatedAt() != null ? fileInfo.getCreatedAt().toString() : "");
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (AppException exception) {
      responseObserver.onNext(GetFileInfoResponse.newBuilder()
          .setFileId(fileId != null ? fileId : "")
          .setFound(false)
          .build());
      responseObserver.onCompleted();
    } catch (Exception exception) {
      log.error("Error fetching file info for fileId {}: {}", fileId, exception.getMessage(), exception);
      responseObserver.onNext(GetFileInfoResponse.newBuilder()
          .setFileId(fileId != null ? fileId : "")
          .setFound(false)
          .build());
      responseObserver.onCompleted();
    }
  }

  @Override
  public void getFileInfos(GetFileInfosRequest request, StreamObserver<GetFileInfosResponse> responseObserver) {
    List<String> fileIds = request.getFileIdsList();
    log.debug("gRPC getFileInfos called for {} fileIds", fileIds != null ? fileIds.size() : 0);

    try {
      GetFileInfosResponse.Builder responseBuilder = GetFileInfosResponse.newBuilder();

      if (fileIds != null && !fileIds.isEmpty()) {
        fileIds.stream()
            .filter(StringUtils::hasText)
            .distinct()
            .forEach(fileId -> addFileInfoIfFound(responseBuilder, fileId));
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception exception) {
      log.error("Error fetching batch file info: {}", exception.getMessage(), exception);
      responseObserver.onNext(GetFileInfosResponse.newBuilder().build());
      responseObserver.onCompleted();
    }
  }

  private void addFileInfoIfFound(GetFileInfosResponse.Builder responseBuilder, String fileId) {
    try {
      FileInfoResponse fileInfo = fileService.getInfo(fileId);
      responseBuilder.putFiles(fileId, FileInfoData.newBuilder()
          .setFileId(fileId)
          .setName(defaultString(fileInfo.getName()))
          .setOriginalName(defaultString(fileInfo.getOriginalName()))
          .setContentType(defaultString(fileInfo.getContentType()))
          .setSize(fileInfo.getSize() != null ? fileInfo.getSize() : 0L)
          .setDownloadUrl(defaultString(fileInfo.getDownloadUrl()))
          .setCreatedAt(fileInfo.getCreatedAt() != null ? fileInfo.getCreatedAt().toString() : "")
          .build());
    } catch (AppException exception) {
      log.debug("File metadata not found for fileId {}", fileId);
    }
  }

  private String defaultString(String value) {
    return value != null ? value : "";
  }
}
