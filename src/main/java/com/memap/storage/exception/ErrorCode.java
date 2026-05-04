package com.memap.storage.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/*
 * Error codes starting with 4 are for storage-service.
 * 4001-4099: Validation errors
 * 4100-4199: Security/Authentication errors
 * 4200-4299: File/Storage errors
 */
@Getter
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

  // Validation errors (4001-4099)
  METHOD_ARGUMENT_NOT_VALID(4001, "Argument not valid", HttpStatus.BAD_REQUEST),
  ROADMAP_NOT_FOUND(4002, "Roadmap not found", HttpStatus.NOT_FOUND),
  ROADMAP_ACCESS_DENIED(4003, "You do not have access to upload files to this roadmap", HttpStatus.FORBIDDEN),

  // Security/Authentication errors (4100-4199)
  UNAUTHENTICATED(4100, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED(4101, "Token has been expired", HttpStatus.UNAUTHORIZED),
  INVALID_TOKEN(4102, "Invalid token", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED(4103, "You do not have permission to access this resource", HttpStatus.FORBIDDEN),
  FORBIDDEN(4104, "You cannot perform this action", HttpStatus.FORBIDDEN),
  INSUFFICIENT_PERMISSIONS(4105, "Insufficient permissions", HttpStatus.FORBIDDEN),

  // File/Storage errors (4200-4299)
  FILE_NOT_FOUND(4200, "File not found", HttpStatus.NOT_FOUND),
  FILE_UPLOAD_FAILED(4201, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_TOO_LARGE(4202, "File size exceeds the maximum limit", HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE(4203, "Invalid file type", HttpStatus.BAD_REQUEST),
  STORAGE_QUOTA_EXCEEDED(4204, "Storage quota exceeded", HttpStatus.BAD_REQUEST),
  STORAGE_ERROR(4205, "Storage error", HttpStatus.INTERNAL_SERVER_ERROR),

  // Roadmap Storage error(4300-)
  ROADMAP_STORAGE_NOT_FOUND(4300, "Roadmap storage not found", HttpStatus.NOT_FOUND),
  STORAGE_LIMIT_EXCEEDED(4301, "Roadmap storage limit exceeded", HttpStatus.BAD_REQUEST);

  ErrorCode(int code, String message, HttpStatusCode statusCode) {
    this.code = code;
    this.message = message;
    this.statusCode = statusCode;
  }

  private final int code;
  private final String message;
  private final HttpStatusCode statusCode;
}
