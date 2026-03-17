package com.memap.storage.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for storage backend operations.
 * Implementations can provide different storage mechanisms (local filesystem,
 * cloud storage, etc.)
 */
public interface IStorageBackend {

  /**
   * Store a file and return its storage path.
   *
   * @param file the multipart file to store
   * @return the relative storage path where the file was stored
   */
  String store(MultipartFile file);

  /**
   * Load a file as a Resource.
   *
   * @param storagePath the relative storage path
   * @return the file as a Resource
   */
  Resource load(String storagePath);

  /**
   * Delete a file from storage.
   *
   * @param storagePath the relative storage path
   * @return true if deletion was successful, false if file didn't exist
   */
  boolean delete(String storagePath);

  /**
   * Initialize the storage backend.
   * Called on application startup to ensure storage is ready.
   */
  void init();
}
