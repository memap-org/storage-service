# Project Context

## Purpose

**MeMap Storage Service** - A microservice for centralized file storage in the MeMap educational platform. The service provides:

- File upload and storage with local filesystem backend
- File metadata management using MongoDB database
- File download and retrieval APIs
- Secure file access with JWT authentication
- Support for multiple storage backends (local, future: MinIO/S3)

This service is intended to be the single source of truth for file operations across all MeMap microservices (roadmap-service, learning-service, profile-service, etc.).

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.5.10
- **Database**: MongoDB (document database for metadata)
- **Authentication**: OAuth2/JWT with Keycloak as identity provider
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Data Access**: Spring Data MongoDB
- **Validation**: Spring Boot Starter Validation
- **Build Tool**: Maven
- **Containerization**: Docker

## Project Conventions

### Code Style

- **Lombok Annotations**: Use `@Builder`, `@Getter`, `@Setter`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`
- **Field Access Level**: Use `@FieldDefaults(level = AccessLevel.PRIVATE)` for private fields by default
- **Builder Defaults**: Use `@Builder.Default` for fields with default values
- **Package Structure**:
  - `entity/` - MongoDB documents
  - `dto/` - Data Transfer Objects (requests/responses)
  - `controller/` - REST controllers
  - `service/` - Service interfaces and implementations
  - `repository/` - MongoDB repositories
  - `exception/` - Custom exceptions and error codes
  - `config/` - Configuration classes
  - `config/security/` - Security configuration
  - `storage/` - Storage backend implementations
  - `model/` - Domain models (non-entity)

### Naming Conventions

- **Entities**: PascalCase matching collection name (e.g., `FileMetadata`, `StorageQuota`)
- **Controllers**: Suffix with `Controller` (e.g., `FileController`)
- **Services**: Interface prefixed with `I`, implementation suffixed with `Impl` or in same package
- **DTOs**: Descriptive names (e.g., `FileUploadRequest`, `FileResponse`)
- **Error Codes**: Numeric codes starting with `4` for storage-service (4001-4099: Validation, 4100-4199: Security, 4200-4299: Storage)

### Architecture Patterns

- **Layered Architecture**: Controller → Service → Repository
- **DTO Pattern**: Separate request/response DTOs from entities
- **Generic API Response**: Use `ApiResponse<T>` wrapper with code, message, and result
- **Exception Handling**: Global exception handler with `ErrorCode` enum
- **Storage Abstraction**: Pluggable storage backends (local filesystem, future cloud storage)

### Testing Strategy

- Spring Boot Test for integration tests
- Test classes in `src/test/java` mirroring main package structure
- Mock security context for authenticated endpoint tests

### Git Workflow

- Feature branches for new features
- Main branch for stable releases
- Use meaningful commit messages

## Domain Context

- **File Metadata**: Information about stored files (name, path, content type, size, checksum, owner)
- **Storage Backend**: Pluggable storage implementation (local filesystem default)
- **File Owner**: User who uploaded the file (from JWT subject)
- **Download URL**: URL pattern for accessing files (configurable prefix)
- **Storage Quota**: Per-user storage limits (planned)

## Important Constraints

- **JWT Authentication Required**: All endpoints require valid JWT token (except public endpoints and Swagger)
- **File Size Limit**: Max 1024MB for file uploads (configurable)
- **REST Port**: REST API on port 8088 with context path `/storage`
- **MongoDB Database**: Document database for metadata storage
- **Local Storage**: Default storage backend at `/data/storage/files`
- **Keycloak Integration**: Uses Keycloak realm roles (TEACHER, STUDENT, ADMIN)

## External Dependencies

- **Keycloak**: Identity provider for OAuth2/JWT authentication
- **MongoDB**: Database for file metadata storage
- **Other MeMap Services**: Roadmap-service, learning-service, profile-service may call storage-service for file operations
