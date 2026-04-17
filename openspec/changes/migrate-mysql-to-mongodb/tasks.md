# Tasks: Migrate from MySQL to MongoDB

## 1. Dependency Changes (pom.xml)

- [x] 1.1 Remove MySQL/JPA/Flyway dependencies
  - Remove `spring-boot-starter-data-jpa`
  - Remove `mysql-connector-j`
  - Remove `flyway-core`
  - Remove `flyway-mysql`
  - Remove `h2` test dependency
- [x] 1.2 Add MongoDB dependencies
  - Add `spring-boot-starter-data-mongodb`
  - Add `de.flapdoodle.embed.mongo.spring3x` as test dependency (for embedded MongoDB in tests)

## 2. Entity Conversion

- [x] 2.1 Convert `FileMetadata` from JPA entity to MongoDB document
  - Replace `@Entity` / `@Table` with `@Document(collection = "file_metadata")`
  - Replace `@Column` annotations with `@Field` annotations (or rely on defaults for camelCase)
  - Replace `@GeneratedValue(strategy = GenerationType.UUID)` with `@Id` + pre-persist UUID generation
  - Replace `@CreationTimestamp` / `@UpdateTimestamp` with `@CreatedDate` / `@LastModifiedDate`
  - Add `@Indexed` annotations on: `ownerId`, `roadmapId`, `roadmapOwnerId`
  - Add `@CompoundIndex` for `{roadmapOwnerId, roadmapId}`
  - **Verify**: all existing fields are preserved with same semantics

## 3. Repository Conversion

- [x] 3.1 Change `FileMetadataRepository` from `JpaRepository` to `MongoRepository`
  - Update interface to extend `MongoRepository<FileMetadata, String>`
  - Keep `findByOwnerId(String ownerId)` — Spring Data MongoDB supports the same method naming
  - Keep `findByOwnerIdOrderByCreatedAtDesc(String ownerId)` — same query derivation support
- [x] 3.2 Rewrite JPQL aggregate queries as MongoDB aggregation
  - Convert `getRoadmapStorageUsageSummary` to `@Aggregation` pipeline:
    - `$match` on `roadmapOwnerId` and `roadmapId != null`
    - `$group` to compute `sum(size)`, `count`, `addToSet(roadmapId)` then `size` for distinct count
  - Convert `findRoadmapStorageUsageItems` to `@Aggregation` pipeline:
    - `$match` on `roadmapOwnerId` and `roadmapId != null`
    - `$group` by `roadmapId` computing `sum(size)`, `count`, `max(createdAt)`
    - `$sort` by `totalBytes` desc, `lastUploadedAt` desc
  - **Verify**: aggregation results match current JPQL output for the same data

## 4. Configuration Changes

- [x] 4.1 Update `src/main/resources/application.yaml`
  - Remove `spring.datasource.*` properties
  - Remove `spring.jpa.*` properties
  - Add `spring.data.mongodb.uri` (e.g., `${STORAGE_MONGO_URI:mongodb://localhost:27017/storage_service}`)
- [x] 4.2 Add MongoDB auditing config
  - Create `MongoConfig` class with `@EnableMongoAuditing`
  - Or add `@EnableMongoAuditing` to the main application class
- [x] 4.3 Update `src/test/resources/application.yaml`
  - Remove H2/MySQL datasource config
  - Remove `spring.jpa.*` and `spring.flyway.*` config
  - Add embedded MongoDB config (flapdoodle auto-configures; minimal config needed)

## 5. Remove Flyway Migrations

- [x] 5.1 Delete SQL migration files
  - Remove `src/main/resources/db/migration/V1__create_file_metadata_table.sql`
  - Remove `src/main/resources/db/migration/V2__add_roadmap_attribution_to_file_metadata.sql`
  - Remove `src/main/resources/db/migration/` directory

## 6. Service Layer Adjustments

- [x] 6.1 Review `FileServiceImpl` for any JPA-specific code
  - Remove `@Transactional` annotations if present (MongoDB single-document operations are atomic)
  - Verify no JPA-specific imports remain (e.g., `javax.persistence`, `jakarta.persistence`)
  - **Verify**: all service methods work unchanged with the MongoDB repository

## 7. Test Updates

- [x] 7.1 Update `FileMetadataRepositoryRoadmapTest`
  - Ensure test works with embedded MongoDB instead of H2
  - Verify aggregation query tests pass with same assertions
- [x] 7.2 Update `StorageServiceApplicationTests`
  - Ensure application context loads with MongoDB config
- [x] 7.3 Update `RoadmapGrpcClientImplTest`
  - Verify no database-specific dependencies
- [ ] 7.4 Run full test suite and fix any remaining failures

## 8. Update Project Documentation

- [x] 8.1 Update `openspec/project.md`
  - Change Tech Stack: MySQL → MongoDB
  - Change ORM: Spring Data JPA → Spring Data MongoDB
  - Update Important Constraints: MySQL → MongoDB
  - Update External Dependencies: MySQL → MongoDB
  - Update package structure: `entity/` description from "JPA entities" to "MongoDB documents"
  - Update repository description from "JPA repositories" to "MongoDB repositories"

## 9. Validation

- [ ] 9.1 Build project successfully (`mvn clean compile`)
- [ ] 9.2 Run all tests (`mvn test`)
- [ ] 9.3 Verify application starts with MongoDB connection
- [ ] 9.4 Smoke-test file upload/download/delete/list endpoints
