# Change: Migrate Storage Backend from Local Filesystem to MinIO

## Why

The storage-service currently uses a local filesystem backend (`LocalStorageBackend`) to store uploaded files. While functional, local storage has significant operational constraints:

1. **No horizontal scaling** — local disk is tied to a single pod/node; file uploads and downloads break when multiple instances run concurrently
2. **No built-in redundancy** — files are lost if the node's disk fails or the container is replaced
3. **Docker volume dependency** — requires a persistent volume mount (`/data/storage/files`) in production, complicating deployment
4. **No object lifecycle management** — no built-in retention, versioning, or archival policies

Migrating to [MinIO](https://min.io/) (an S3-compatible object storage server) solves all of the above while preserving the existing API contract and `IStorageBackend` abstraction already in place.

## Migration Scope

This change is split into three sequential phases:

| Phase | Scope | Prerequisite |
|---|---|---|
| **0 — Infrastructure** | Provision MinIO via Docker Compose; expose API + console ports; configure named volume | None |
| **1 — Application** | Add MinIO SDK; implement `MinioStorageBackend`; wire conditional beans; update config | Phase 0 complete |
| **2 — Validation** | Unit tests, optional Testcontainers integration test, regression check | Phase 1 complete |

## What Changes

### Phase 0 — Infrastructure (Docker Compose)

A new `docker-compose.yml` is introduced at the storage-service root to run MinIO alongside the service.

#### MinIO Service

```yaml
services:
  minio:
    image: minio/minio:latest
    container_name: minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"   # S3-compatible API
      - "9001:9001"   # MinIO web console
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 30s
      timeout: 20s
      retries: 5
    networks:
      - memap-network

volumes:
  minio_data:

networks:
  memap-network:
    external: true
```

- **API port** `9000` — used by `MinioClient` in the application
- **Console port** `9001` — MinIO web UI for bucket inspection during development
- **Named volume** `minio_data` — survives container restarts; no bind-mount to host path required
- **Health check** — verifies MinIO readiness before the storage-service starts
- **Network** — joins the existing `memap-network` so storage-service and MinIO can communicate by service name

#### Environment Variables Added to `.env`

```dotenv
# ===== MinIO =====
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
STORAGE_MINIO_ENDPOINT=http://minio:9000
STORAGE_MINIO_ACCESS_KEY=minioadmin
STORAGE_MINIO_SECRET_KEY=minioadmin
STORAGE_MINIO_BUCKET=memap-files
```

> **Security note**: `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` are the MinIO root credentials used to boot the server. `STORAGE_MINIO_ACCESS_KEY`/`STORAGE_MINIO_SECRET_KEY` are the credentials injected into the Java `MinioClient`. For production, create a dedicated MinIO service account with bucket-scoped policies instead of using root credentials.

---

### Phase 1 — Application

#### New Dependency (pom.xml)

- **ADD** `io.minio:minio` (MinIO Java SDK)

#### New Storage Backend

- **ADD** `MinioStorageBackend` — implements `IStorageBackend`, stores/retrieves/deletes objects from a MinIO bucket
  - Object key format: `{year}/{month}/{uuid}.{ext}` (same as current local path pattern)
  - `store()` — upload via `MinioClient.putObject()`
  - `load()` — retrieve as `InputStreamResource` via `MinioClient.getObject()`
  - `delete()` — remove via `MinioClient.removeObject()`
  - `init()` — verify bucket exists; create it if absent (private, no public policy)

#### Configuration (`StorageConfig`)

- **ADD** `MinioProperties` nested class:
  - `endpoint` — MinIO server URL (e.g., `http://minio:9000`)
  - `accessKey` — MinIO access key
  - `secretKey` — MinIO secret key
  - `bucketName` — target bucket name
- **MODIFY** `StorageConfig` to include `minio` nested config block
- **MODIFY** `application.yaml` — add `app.storage.minio.*` properties; change `app.storage.backend` default to `minio`

#### Backend Selection (Conditional Beans)

- **MODIFY** `LocalStorageBackend` — add `@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local")` so it only activates when explicitly configured
- **ADD** `MinioStorageBackend` with `@ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio", matchIfMissing = true)` as the new default

#### No Changes Required

- `IStorageBackend` interface — no changes (abstraction already covers MinIO operations)
- `FileServiceImpl` — no changes (depends on `IStorageBackend` interface only)
- `FileMetadata` entity — no changes (`storagePath` field stores the object key, same semantics)
- REST controllers and DTOs — no changes
- Download URL strategy — unchanged; file access stays service-routed (client calls `/file/{id}/download`, service streams from MinIO after JWT validation)
- MongoDB configuration — no changes
- gRPC layer — no changes

---

### Phase 2 — Validation

- Unit tests for `MinioStorageBackend` with a mocked `MinioClient`
- Optional Testcontainers integration test using the `minio/minio` Docker image
- Regression check: existing tests still pass with `app.storage.backend=local`

## Impact

- **Affected specs**:
  - `implement-file-local-storage/specs/file-local-storage` — storage backend requirement modified (new impl, same interface)
- **Affected files**:
  - `docker-compose.yml` — NEW: MinIO service, named volume, network
  - `.env` — add MinIO root + client credentials
  - `pom.xml` — add MinIO SDK
  - `config/StorageConfig.java` — add `MinioProperties` nested class
  - `config/MinioClientConfig.java` — NEW: `@Bean MinioClient`
  - `storage/MinioStorageBackend.java` — NEW
  - `storage/LocalStorageBackend.java` — add `@ConditionalOnProperty`
  - `src/main/resources/application.yaml` — add MinIO config block, change default backend
- **Breaking changes**: **BREAKING for deployment** — requires a running MinIO instance; existing files on local disk are not automatically migrated
- **Data migration**: Files already stored locally must be manually or programmatically uploaded to the MinIO bucket. A one-time migration script is out of scope for this change but should be planned before switching production traffic.

## Risks and Mitigations

| Risk                                                | Mitigation                                                                                                     |
| --------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| MinIO server unavailable at startup                 | `init()` logs a warning but does not crash the app; first request will fail with `FILE_UPLOAD_FAILED`          |
| Credential exposure in config                       | Use environment variables; never hardcode credentials in `application.yaml`                                    |
| Existing local files not accessible after migration | Document that `app.storage.backend=local` can be restored temporarily; provide migration guidance              |
| Larger test surface (MinIO calls)                   | Use Testcontainers MinIO image in integration tests; unit-test `MinioStorageBackend` with a mock `MinioClient` |
| Network latency vs local disk                       | Acceptable trade-off; MinIO can be co-located in the same cluster                                              |
