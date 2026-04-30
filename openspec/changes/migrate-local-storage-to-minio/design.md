# Design: Migrate Storage Backend from Local Filesystem to MinIO

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                 Docker Network (memap-network)   │
│                                                  │
│  ┌──────────────────┐      ┌──────────────────┐  │
│  │  storage-service │─────▶│     MinIO        │  │
│  │  :8088 / :9088   │      │  API  :9000      │  │
│  │                  │      │  Console :9001   │  │
│  │  MinioStorageBackend     │                  │  │
│  │    putObject()   │      │  Bucket:         │  │
│  │    getObject()   │      │  memap-files     │  │
│  │    removeObject()│      │                  │  │
│  └──────────────────┘      └────────┬─────────┘  │
│                                     │             │
└─────────────────────────────────────┼─────────────┘
                                      │
                               ┌──────▼────────┐
                               │  minio_data   │
                               │ (named volume)│
                               └───────────────┘
```

## Key Decisions

### 1. MinIO in the Same Docker Network

`minio` runs as a Docker Compose service in the same `memap-network` as the application. This allows the Java `MinioClient` to address it by service name (`http://minio:9000`) without exposing MinIO on a public interface.

**Alternative considered**: Managed S3-compatible storage (e.g., AWS S3, Cloudflare R2). Rejected for local/dev parity — MinIO is fully S3-compatible so switching to managed S3 later requires only credential changes.

### 2. Backend Selection via `@ConditionalOnProperty`

The `app.storage.backend` property controls which `IStorageBackend` bean Spring activates:

| `app.storage.backend` | Active bean |
|---|---|
| `minio` (or unset) | `MinioStorageBackend` |
| `local` | `LocalStorageBackend` |

This ensures zero-code-change fallback to local storage during development or when MinIO is unavailable.

### 3. Object Key Format: `{year}/{month}/{uuid}.{ext}`

Matches the existing local path format exactly. The same value is stored in `FileMetadata.storagePath` regardless of backend — no MongoDB migration required.

### 4. Bucket Initialization in `init()` (not at startup fail-fast)

`MinioStorageBackend.init()` creates the bucket if absent and logs a warning on failure, but does **not** throw — avoiding container crash-loops when MinIO is temporarily unreachable. The first actual upload will surface the error via `FILE_UPLOAD_FAILED`.

**Trade-off**: First request fails instead of startup. Acceptable given that MinIO has a health check and `depends_on: service_healthy` in Docker Compose.

### 5. MinIO Console Exposed on Port 9001 (dev only)

The console port `9001` is mapped to the host to allow developers to browse buckets and objects during development. In production, remove the host-side port mapping and access the console only via an internal network or VPN tunnel.

### 6. Credentials via Environment Variables Only

Root MinIO credentials (`MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`) and the Java client credentials (`STORAGE_MINIO_ACCESS_KEY` / `STORAGE_MINIO_SECRET_KEY`) are always injected via `.env` / environment variables. They are never hardcoded in `application.yaml` or `docker-compose.yml`.

For production, replace root credentials with a dedicated MinIO service account configured with a bucket-scoped read/write policy.

## Migration Sequence

```
1. Provision MinIO (Phase 0)
   ↓
2. Verify MinIO health via console/health probe
   ↓
3. Deploy storage-service with app.storage.backend=minio (Phase 1)
   ↓
4. Smoke-test upload/download/delete via REST API
   ↓
5. (Out of scope) Migrate existing local files to MinIO bucket if needed
```

## Files Affected

| File | Change |
|---|---|
| `docker-compose.yml` | NEW — MinIO service, volume, network |
| `.env` | ADD MinIO root + client env vars |
| `pom.xml` | ADD `io.minio:minio` dependency |
| `config/StorageConfig.java` | ADD `MinioProperties` nested class |
| `config/MinioClientConfig.java` | NEW — `@Bean MinioClient` |
| `storage/MinioStorageBackend.java` | NEW — IStorageBackend impl |
| `storage/LocalStorageBackend.java` | ADD `@ConditionalOnProperty` guard |
| `src/main/resources/application.yaml` | ADD minio config block; change default backend |
