package com.memap.storage.repository;

import com.memap.storage.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

  List<FileMetadata> findByOwnerId(String ownerId);

  List<FileMetadata> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
