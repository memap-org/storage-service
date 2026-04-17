# Change: Migrate from MySQL to MongoDB

## Why

The storage-service currently uses MySQL (relational) for file metadata storage. The `FileMetadata` document is a single, flat entity with no relational joins, foreign keys, or complex transactional requirements — making it a natural fit for a document database. Migrating to MongoDB will:

1. **Simplify schema evolution** — adding fields requires no migrations; MongoDB is schema-flexible
2. **Eliminate Flyway overhead** — no SQL migration files to maintain for a single-collection schema
3. **Better fit for document-oriented data** — file metadata is self-contained with no relational links
4. **Simplify aggregation queries** — MongoDB's aggregation pipeline is a natural fit for the roadmap storage usage queries currently expressed as JPQL
5. **Align with microservice best practices** — lightweight document store for a service that stores metadata about files, not relational data

## What Changes

### Dependencies (pom.xml)

- **REMOVE** `spring-boot-starter-data-jpa`
- **REMOVE** `mysql-connector-j`
- **REMOVE** `flyway-core`, `flyway-mysql`
- **REMOVE** `h2` test dependency
- **ADD** `spring-boot-starter-data-mongodb`
- **ADD** `de.flapdoodle.embed.mongo.spring3x` (test dependency for embedded MongoDB)

### Entity → Document

- **MODIFY** `FileMetadata` — replace JPA annotations (`@Entity`, `@Table`, `@Column`, `@GeneratedValue`) with MongoDB annotations (`@Document`, `@Field`, `@Id`, `@Indexed`)
- ID generation switches from `GenerationType.UUID` to a custom or MongoDB-native strategy
- Timestamp annotations switch from Hibernate `@CreationTimestamp`/`@UpdateTimestamp` to Spring Data `@CreatedDate`/`@LastModifiedDate` with `@EnableMongoAuditing`

### Repository

- **MODIFY** `FileMetadataRepository` — change from `JpaRepository<FileMetadata, String>` to `MongoRepository<FileMetadata, String>`
- **REWRITE** JPQL aggregate queries (`getRoadmapStorageUsageSummary`, `findRoadmapStorageUsageItems`) as MongoDB `@Aggregation` pipelines or custom repository methods

### Configuration

- **MODIFY** `application.yaml` — replace `spring.datasource.*` and `spring.jpa.*` with `spring.data.mongodb.uri` (or `host`/`port`/`database`)
- **REMOVE** Flyway configuration
- **ADD** MongoDB index initialization (via `@Indexed` annotations or a config class)

### Migrations

- **REMOVE** `src/main/resources/db/migration/V1__create_file_metadata_table.sql`
- **REMOVE** `src/main/resources/db/migration/V2__add_roadmap_attribution_to_file_metadata.sql`

### Test Configuration

- **MODIFY** `src/test/resources/application.yaml` — replace H2/MySQL datasource with embedded MongoDB config
- **UPDATE** existing tests to work with MongoDB (repository tests, integration tests)

### No Changes Required

- REST controllers — no changes (they depend on service layer, not database layer)
- Service layer — no changes to interface; implementation changes only for transaction annotations if present
- DTOs, models, error codes — no changes
- Storage backend (local filesystem) — no changes
- gRPC client — no changes
- Security configuration — no changes

## Impact

- **Affected specs**: `file-metadata` (storage mechanism changes from MySQL to MongoDB)
- **Affected code**:
  - `pom.xml` — dependency swap
  - `entity/FileMetadata.java` — annotation replacement
  - `repository/FileMetadataRepository.java` — interface + query rewrite
  - `config/` — add `MongoConfig` or auditing config
  - `application.yaml` (main + test) — database connection config
  - `db/migration/*.sql` — removed
  - Test classes — adapt to embedded MongoDB
- **Breaking changes**: **BREAKING** — requires MongoDB instance instead of MySQL; deployment configuration must change
- **Data migration**: Existing MySQL data must be migrated to MongoDB (one-time migration script or manual export/import)

## Risks and Mitigations

| Risk                            | Mitigation                                                                         |
| ------------------------------- | ---------------------------------------------------------------------------------- |
| Data loss during migration      | Export MySQL data before migration; write a one-time migration script              |
| Aggregation query correctness   | Thoroughly test MongoDB aggregation pipelines against known MySQL results          |
| Embedded MongoDB test flakiness | Pin flapdoodle version; use Testcontainers as fallback                             |
| Deployment coordination         | Update Docker Compose / Kubernetes manifests to provision MongoDB instead of MySQL |
| Index coverage                  | Define indexes explicitly via `@Indexed` to match current MySQL indexes            |
