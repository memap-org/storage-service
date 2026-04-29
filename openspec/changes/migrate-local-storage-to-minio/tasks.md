# Tasks: Migrate Storage Backend from Local Filesystem to MinIO

> Phases must be completed in order: **Phase 0 → Phase 1 → Phase 2**.
> Items within a phase can be parallelized unless noted.

---

## Phase 0 — Infrastructure

### 0.1 Create `docker-compose.yml` for storage-service

Create `storage-service/docker-compose.yml` with:

- `minio` service using `minio/minio:latest`
  - command: `server /data --console-address ":9001"`
  - ports: `9000:9000` (S3 API), `9001:9001` (console)
  - environment: `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` (from `.env`)
  - named volume: `minio_data:/data`
  - health check: `mc ready local` (30 s interval, 5 retries)
  - network: `memap-network` (external)
- `storage-service` app service (existing Dockerfile)
  - depends_on: `minio` with condition `service_healthy`
  - env_file: `.env`
  - network: `memap-network` (external)
- `volumes:` block declaring `minio_data`
- `networks:` block declaring `memap-network` as external

### 0.2 Update `.env` with MinIO variables

Add the following to `.env`:

```dotenv
# ===== MinIO =====
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
STORAGE_MINIO_ENDPOINT=http://minio:9000
STORAGE_MINIO_ACCESS_KEY=minioadmin
STORAGE_MINIO_SECRET_KEY=minioadmin
STORAGE_MINIO_BUCKET=memap-files
```

> For production: replace root credentials with a dedicated MinIO service account.

### 0.3 Smoke-test MinIO via docker-compose

- [ ] Run `docker compose up minio -d` and verify MinIO console is reachable at `http://localhost:9001`
- [ ] Confirm API port `http://localhost:9000` responds with `200` to a health probe (`curl http://localhost:9000/minio/health/live`)
- [ ] Verify `minio_data` named volume is created: `docker volume ls`

---

## Phase 1 — Application

### 1. Dependency

- [x] 1.1 Add MinIO Java SDK to `pom.xml`
  - Add `io.minio:minio` (latest stable, e.g., `8.5.x`)
  - Verify no version conflicts with existing gRPC/protobuf dependencies

### 2. Configuration

- [x] 2.1 Add `MinioProperties` nested class to `StorageConfig`
  - Fields: `endpoint`, `accessKey`, `secretKey`, `bucketName`
  - Bind via `app.storage.minio.*`
- [x] 2.2 Create `config/MinioClientConfig.java`
  - `@Configuration` class with a `@Bean MinioClient` method
  - Use `@ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio", matchIfMissing = true)`
  - Build via `MinioClient.builder().endpoint(...).credentials(...).build()`
- [x] 2.3 Update `src/main/resources/application.yaml`
  - Add `app.storage.minio:` block with env-var placeholders (`${STORAGE_MINIO_ENDPOINT}`, etc.)
  - Change `app.storage.backend` default value from `local` to `minio`

### 3. MinIO Storage Backend

- [x] 3.1 Create `storage/MinioStorageBackend.java` implementing `IStorageBackend`
  - Annotate with `@Component` and `@ConditionalOnProperty(name = "app.storage.backend", havingValue = "minio", matchIfMissing = true)`
  - Inject `MinioClient` and `StorageConfig`
  - `init()`: verify bucket exists; create if absent (private); log result
  - `store()`: upload using `MinioClient.putObject()`; return object key as storage path; object key pattern: `{year}/{month}/{uuid}.{ext}`
  - `load()`: fetch object via `MinioClient.getObject()`; wrap in `InputStreamResource`; throw `FILE_NOT_FOUND` if object is absent
  - `delete()`: remove via `MinioClient.removeObject()`; return `true` on success, `false` if object did not exist

### 4. Local Backend Guard

- [x] 4.1 Add `@ConditionalOnProperty(name = "app.storage.backend", havingValue = "local")` to `LocalStorageBackend`
  - Prevents bean conflict when MinIO backend is the active profile

---

## Phase 2 — Validation

### 5. Tests

- [x] 5.1 Write unit tests for `MinioStorageBackend`
  - Mock `MinioClient` to test `store()`, `load()`, `delete()`, and `init()` paths
  - Assert `FILE_NOT_FOUND` is thrown when object does not exist on `load()`
  - Assert `FILE_UPLOAD_FAILED` is thrown on MinIO error during `store()`
- [ ] 5.2 Add Testcontainers MinIO integration test (optional but recommended)
  - Use `minio/minio` container image
  - Verify full upload → download → delete cycle end-to-end
- [x] 5.3 Verify existing tests still pass with `app.storage.backend=local` in `src/test/resources/application.yaml`
  - Confirm `LocalStorageBackend` is still the active bean during tests (no MinIO required)

---

## Dependencies and Notes

- Phase 0 must be complete before testing Phase 1 against a live MinIO instance
- Tasks 3.1 and 2.2 can be developed in parallel with 4.1
- Task 5.3 must run after Phase 1 to confirm no bean wiring regressions
- **Do not** remove `LocalStorageBackend`; it remains available for local development and fallback
- **No data migration** is in scope; files stored locally before this change are not moved to MinIO automatically
- For production switch-over: keep `app.storage.backend=local` until MinIO is provisioned and confirmed healthy; then flip the environment variable and restart the service
