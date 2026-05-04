package com.memap.storage.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.memap.storage.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception) {
    log.info(exception.getMessage());
    ApiResponse apiResponse = ApiResponse.builder()
        .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
        .build();
    return ResponseEntity.badRequest().body(apiResponse);
  }

  @ExceptionHandler(AppException.class)
  ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    ApiResponse apiResponse = ApiResponse.builder()
        .code(errorCode.getCode())
        .message(errorCode.getMessage())
        .build();
    return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
    ApiResponse apiResponse = new ApiResponse();
    apiResponse.setCode(ErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode());

    String defaultMessage = ErrorCode.METHOD_ARGUMENT_NOT_VALID.getMessage();
    FieldError fieldError = ex.getFieldError();
    if (fieldError == null) {
      apiResponse.setMessage(defaultMessage);
      return ResponseEntity.badRequest().body(apiResponse);
    }

    String errorMessage = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .toList()
        .toString();
    errorMessage = errorMessage.isBlank() ? defaultMessage : errorMessage;
    apiResponse.setMessage(errorMessage);
    return ResponseEntity.badRequest().body(apiResponse);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
    String message = ErrorCode.METHOD_ARGUMENT_NOT_VALID.getMessage();

    Throwable cause = ex.getCause();
    if (cause instanceof InvalidFormatException ife) {
      if (ife.getTargetType().isEnum()) {
        Object invalidValue = ife.getValue();
        Object[] acceptedValues = ife.getTargetType().getEnumConstants();

        message = String.format(
            "Invalid value '%s'. Accepted values are: %s",
            invalidValue,
            java.util.Arrays.toString(acceptedValues));
      }
    }

    ApiResponse apiResponse = ApiResponse.builder()
        .code(ErrorCode.METHOD_ARGUMENT_NOT_VALID.getCode())
        .message(message)
        .build();
    return ResponseEntity.badRequest().body(apiResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException exception) {
    log.warn("Access denied: {}", exception.getMessage());
    ApiResponse apiResponse = ApiResponse.builder()
        .code(ErrorCode.ACCESS_DENIED.getCode())
        .message(ErrorCode.ACCESS_DENIED.getMessage())
        .build();
    return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatusCode()).body(apiResponse);
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ApiResponse> handleAuthorizationDeniedException(AuthorizationDeniedException exception) {
    log.warn("Authorization denied: {}", exception.getMessage());
    ApiResponse apiResponse = ApiResponse.builder()
        .code(ErrorCode.FORBIDDEN.getCode())
        .message(ErrorCode.FORBIDDEN.getMessage())
        .build();
    return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatusCode()).body(apiResponse);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
    log.warn("File size exceeded: {}", exception.getMessage());
    ApiResponse apiResponse = ApiResponse.builder()
        .code(ErrorCode.FILE_TOO_LARGE.getCode())
        .message(ErrorCode.FILE_TOO_LARGE.getMessage())
        .build();
    return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getStatusCode()).body(apiResponse);
  }
}
