package com.memap.storage.service;

import com.memap.storage.dto.FileInfoResponse;
import com.memap.storage.dto.FileUploadResponse;
import com.memap.storage.entity.FileMetadata;
import org.springframework.core.io.Resource;
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
}
