package com.memap.storage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.memap.storage.entity.FileMetadata;
import com.memap.storage.model.RoadmapAssetType;
import com.memap.storage.model.RoadmapStorageUsageItem;
import com.memap.storage.model.RoadmapStorageUsageSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@EnableMongoAuditing
class FileMetadataRepositoryRoadmapTest {

    @Autowired
    private FileMetadataRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldPersistAndRetrieveRoadmapAttributionFields() {
        FileMetadata metadata = FileMetadata.builder()
                .name("test-file.png")
                .originalName("photo.png")
                .contentType("image/png")
                .size(1024L)
                .md5Checksum("abc123def456abc123def456abc12345")
                .storagePath("/data/storage/files/test-file.png")
                .ownerId("user-1")
                .roadmapId("rm-100")
                .roadmapOwnerId("owner-1")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_IMAGE.name())
                .build();

        FileMetadata saved = repository.save(metadata);

        FileMetadata found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRoadmapId()).isEqualTo("rm-100");
        assertThat(found.getRoadmapOwnerId()).isEqualTo("owner-1");
        assertThat(found.getRoadmapAssetType()).isEqualTo(RoadmapAssetType.ROADMAP_IMAGE.name());
    }

    @Test
    void shouldPersistFileMetadataWithoutRoadmapContext() {
        FileMetadata metadata = FileMetadata.builder()
                .name("avatar.jpg")
                .originalName("my-avatar.jpg")
                .contentType("image/jpeg")
                .size(2048L)
                .md5Checksum("def456abc123def456abc123def45678")
                .storagePath("/data/storage/files/avatar.jpg")
                .ownerId("user-2")
                .build();

        FileMetadata saved = repository.save(metadata);

        FileMetadata found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRoadmapId()).isNull();
        assertThat(found.getRoadmapOwnerId()).isNull();
        assertThat(found.getRoadmapAssetType()).isNull();
    }

    @Test
    void shouldFindByOwnerIdIncludingRoadmapFields() {
        repository.save(FileMetadata.builder()
                .name("file-1.png")
                .originalName("image1.png")
                .contentType("image/png")
                .size(500L)
                .md5Checksum("aaaa1111bbbb2222cccc3333dddd4444")
                .storagePath("/data/storage/files/file-1.png")
                .ownerId("user-3")
                .roadmapId("rm-200")
                .roadmapOwnerId("user-3")
                .roadmapAssetType(RoadmapAssetType.NODE_RESOURCE.name())
                .build());

        repository.save(FileMetadata.builder()
                .name("file-2.pdf")
                .originalName("doc.pdf")
                .contentType("application/pdf")
                .size(3000L)
                .md5Checksum("eeee5555ffff6666aaaa7777bbbb8888")
                .storagePath("/data/storage/files/file-2.pdf")
                .ownerId("user-3")
                .roadmapId("rm-200")
                .roadmapOwnerId("user-3")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_RESOURCE.name())
                .build());

        List<FileMetadata> files = repository.findByOwnerId("user-3");
        assertThat(files).hasSize(2);
        assertThat(files).allSatisfy(f -> {
            assertThat(f.getRoadmapId()).isEqualTo("rm-200");
            assertThat(f.getRoadmapOwnerId()).isEqualTo("user-3");
            assertThat(f.getRoadmapAssetType()).isNotNull();
        });
    }

    @Test
    void shouldAggregateRoadmapStorageSummaryByRoadmapOwner() {
        repository.save(FileMetadata.builder()
                .name("summary-1.png")
                .originalName("summary-1.png")
                .contentType("image/png")
                .size(1024L)
                .md5Checksum("11111111111111111111111111111111")
                .storagePath("/data/storage/files/summary-1.png")
                .ownerId("uploader-1")
                .roadmapId("rm-301")
                .roadmapOwnerId("owner-summary")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_IMAGE.name())
                .build());

        repository.save(FileMetadata.builder()
                .name("summary-2.pdf")
                .originalName("summary-2.pdf")
                .contentType("application/pdf")
                .size(2048L)
                .md5Checksum("22222222222222222222222222222222")
                .storagePath("/data/storage/files/summary-2.pdf")
                .ownerId("uploader-2")
                .roadmapId("rm-301")
                .roadmapOwnerId("owner-summary")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_RESOURCE.name())
                .build());

        repository.save(FileMetadata.builder()
                .name("summary-3.mp4")
                .originalName("summary-3.mp4")
                .contentType("video/mp4")
                .size(4096L)
                .md5Checksum("33333333333333333333333333333333")
                .storagePath("/data/storage/files/summary-3.mp4")
                .ownerId("uploader-2")
                .roadmapId("rm-302")
                .roadmapOwnerId("owner-summary")
                .roadmapAssetType(RoadmapAssetType.NODE_INLINE_MEDIA.name())
                .build());

        repository.save(FileMetadata.builder()
                .name("not-attributed.txt")
                .originalName("not-attributed.txt")
                .contentType("text/plain")
                .size(8192L)
                .md5Checksum("44444444444444444444444444444444")
                .storagePath("/data/storage/files/not-attributed.txt")
                .ownerId("uploader-3")
                .roadmapOwnerId("owner-summary")
                .build());

        RoadmapStorageUsageSummary summary = repository.getRoadmapStorageUsageSummary("owner-summary");

        assertThat(summary.getTotalBytes()).isEqualTo(7168L);
        assertThat(summary.getTotalFiles()).isEqualTo(3L);
        assertThat(summary.getRoadmapCount()).isEqualTo(2L);
        assertThat(summary.hasUsage()).isTrue();
    }

    @Test
    void shouldReturnPerRoadmapUsageItemsOrderedByTotalBytes() {
        LocalDateTime firstUpload = LocalDateTime.of(2026, 3, 20, 10, 0);
        LocalDateTime latestUpload = LocalDateTime.of(2026, 3, 21, 11, 30);

        repository.save(FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .name("roadmap-a-1.png")
                .originalName("roadmap-a-1.png")
                .contentType("image/png")
                .size(1000L)
                .md5Checksum("55555555555555555555555555555555")
                .storagePath("/data/storage/files/roadmap-a-1.png")
                .ownerId("uploader-a")
                .roadmapId("rm-a")
                .roadmapOwnerId("owner-items")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_IMAGE.name())
                .createdAt(firstUpload)
                .updatedAt(firstUpload)
                .build());

        repository.save(FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .name("roadmap-a-2.pdf")
                .originalName("roadmap-a-2.pdf")
                .contentType("application/pdf")
                .size(2000L)
                .md5Checksum("66666666666666666666666666666666")
                .storagePath("/data/storage/files/roadmap-a-2.pdf")
                .ownerId("uploader-a")
                .roadmapId("rm-a")
                .roadmapOwnerId("owner-items")
                .roadmapAssetType(RoadmapAssetType.ROADMAP_RESOURCE.name())
                .createdAt(latestUpload)
                .updatedAt(latestUpload)
                .build());

        repository.save(FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .name("roadmap-b-1.pdf")
                .originalName("roadmap-b-1.pdf")
                .contentType("application/pdf")
                .size(500L)
                .md5Checksum("77777777777777777777777777777777")
                .storagePath("/data/storage/files/roadmap-b-1.pdf")
                .ownerId("uploader-b")
                .roadmapId("rm-b")
                .roadmapOwnerId("owner-items")
                .roadmapAssetType(RoadmapAssetType.NODE_RESOURCE.name())
                .createdAt(firstUpload.plusHours(2))
                .updatedAt(firstUpload.plusHours(2))
                .build());

        repository.save(FileMetadata.builder()
                .id(UUID.randomUUID().toString())
                .name("ignored.bin")
                .originalName("ignored.bin")
                .contentType("application/octet-stream")
                .size(9000L)
                .md5Checksum("88888888888888888888888888888888")
                .storagePath("/data/storage/files/ignored.bin")
                .ownerId("uploader-c")
                .roadmapOwnerId("owner-items")
                .build());

        List<RoadmapStorageUsageItem> usageItems = repository.findRoadmapStorageUsageItems("owner-items");

        assertThat(usageItems).hasSize(2);

        RoadmapStorageUsageItem firstItem = usageItems.get(0);
        assertThat(firstItem.getRoadmapId()).isEqualTo("rm-a");
        assertThat(firstItem.getTotalBytes()).isEqualTo(3000L);
        assertThat(firstItem.getFileCount()).isEqualTo(2L);
        assertThat(firstItem.getLastUploadedAt()).isEqualTo(latestUpload);

        RoadmapStorageUsageItem secondItem = usageItems.get(1);
        assertThat(secondItem.getRoadmapId()).isEqualTo("rm-b");
        assertThat(secondItem.getTotalBytes()).isEqualTo(500L);
        assertThat(secondItem.getFileCount()).isEqualTo(1L);
    }
}
