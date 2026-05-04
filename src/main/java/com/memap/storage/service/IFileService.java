package com.memap.storage.service;

import com.memap.storage.dto.response.FileInfoResponse;
import com.memap.storage.dto.response.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import com.memap.storage.model.RoadmapStorageUsageItem;
import com.memap.storage.model.RoadmapStorageUsageSummary;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileService {

  /**
   * Upload a file and store its metadata.
   *
   * @param file       the file to upload
   * @param customName optional custom name for the file
   * @return upload response with file details
   */
  FileUploadResponse upload(MultipartFile file, String customName);

  /**
   * Upload a file with optional roadmap context.
   * When roadmapId is provided the roadmap's existence and the caller's access
   * are validated via gRPC before storing the file.
   *
   * @param file             the file to upload
   * @param customName       optional custom name for the file
   * @param roadmapId        optional roadmap identifier
   * @param roadmapAssetType optional roadmap asset type
   * @return upload response with file details
   */
  FileUploadResponse upload(MultipartFile file, String customName, String roadmapId, String roadmapAssetType);

  /**
   * Upload a file scoped to a specific roadmap.
   * Validates roadmap existence and caller's access via gRPC.
   *
   * @param file             the file to upload
   * @param customName       optional custom display name
   * @param roadmapId        the roadmap the file belongs to
   * @param roadmapAssetType the asset type within the roadmap
   * @return upload response with file details
   */
  FileUploadResponse uploadForRoadmap(MultipartFile file, String customName,
      String roadmapId, String roadmapAssetType);

  /**
   * Download a file by its ID.
   *
   * @param fileId the file ID
   * @return the file resource
   */
  Resource download(String fileId);

  /**
   * Get file metadata by ID.
   *
   * @param fileId the file ID
   * @return file metadata
   */
  FileMetadata getFileMetadata(String fileId);

  /**
   * Get file information by ID.
   *
   * @param fileId the file ID
   * @return file info response
   */
  FileInfoResponse getInfo(String fileId);

  /**
   * Get all files owned by the current user.
   *
   * @return list of file info responses
   */
  List<FileInfoResponse> getMyFiles();

  /**
   * Delete a file by ID. Only the owner can delete.
   *
   * @param fileId the file ID
   */
  void delete(String fileId);

  /**
   * Delete a file by ID without owner check. For internal service-to-service calls only.
   *
   * @param fileId the file ID
   */
  void deleteInternal(String fileId);

  /**
   * Get aggregate storage usage summary across all roadmaps owned by the given
   * user.
   *
   * @param roadmapOwnerId the ID of the roadmap owner
   * @return aggregate summary (total bytes, total files, roadmap count)
   */
  RoadmapStorageUsageSummary getRoadmapStorageUsageSummary(String roadmapOwnerId);

  /**
   * Get per-roadmap storage usage breakdown for the given roadmap owner.
   *
   * @param roadmapOwnerId the ID of the roadmap owner
   * @return list of per-roadmap usage items ordered by last-upload date
   *         descending
   */
  List<RoadmapStorageUsageItem> getRoadmapStorageUsageItems(String roadmapOwnerId);

  /**
   * Get aggregate storage usage summary for the authenticated user's roadmaps.
   *
   * @return aggregate summary (total bytes, total files, roadmap count)
   */
  RoadmapStorageUsageSummary getMyRoadmapStorageUsageSummary();

  /**
   * Get per-roadmap storage usage breakdown for the authenticated user's
   * roadmaps.
   *
   * @return list of per-roadmap usage items ordered by total bytes descending
   */
  List<RoadmapStorageUsageItem> getMyRoadmapStorageUsageItems();

  Page<FileInfoResponse> getStorageByRoadmapId(String roadmapId, String search, Pageable pageable);

  /**
   * Get all files uploaded by the authenticated user for a specific roadmap.
   *
   * @param roadmapId the roadmap identifier
   * @return list of file info responses ordered by creation date (newest first)
   */
  List<FileInfoResponse> getMyFilesByRoadmapId(String roadmapId);
}
