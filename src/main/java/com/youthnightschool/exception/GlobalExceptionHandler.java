package com.youthnightschool.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler mirroring the NestJS GlobalExceptionFilter.
 *
 * <p>Response format matches exactly:
 * <pre>
 *   Success: { "success": true, "data": ..., "message": "..." }
 *   Error:   { "success": false, "message": "...", "path": "..." }
 * </pre>
 *
 * <p>In non-production profiles, a {@code error} detail field is included for non-5xx errors.
 * 5xx errors never expose details in any environment.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Value("${spring.profiles.active:production}")
  private String activeProfile;

  private boolean isProduction() {
    return activeProfile != null
        && (activeProfile.contains("production") || activeProfile.contains("prod"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("；"));

    if (message == null || message.isEmpty()) {
      message = "请求参数验证失败";
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", message);
    body.put("path", request.getRequestURI());

    if (!isProduction()) {
      body.put("error", message);
    }

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {

    log.warn("Data integrity violation: {}", ex.getMessage());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", "数据冲突，请重试");
    body.put("path", request.getRequestURI());

    if (!isProduction()) {
      body.put("error", "数据冲突，请重试");
    }

    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", "没有权限访问");
    body.put("path", request.getRequestURI());

    if (!isProduction()) {
      body.put("error", "没有权限访问");
    }

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {

    String message = ex.getMessage();
    if (message == null || message.isEmpty()) {
      message = "请求参数错误";
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", message);
    body.put("path", request.getRequestURI());

    if (!isProduction()) {
      body.put("error", message);
    }

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>> handleRuntime(
      RuntimeException ex, HttpServletRequest request) {

    String message = ex.getMessage();
    if (message == null || message.isEmpty()) {
      message = "服务器内部错误";
    }

    log.warn("Runtime exception: {}", message);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", message);
    body.put("path", request.getRequestURI());

    // Treat service-layer runtime exceptions as 400 (Bad Request) by default,
    // matching NestJS behavior where service exceptions are typically BadRequestException.
    HttpStatus status = HttpStatus.BAD_REQUEST;

    if (!isProduction()) {
      body.put("error", message);
    }

    return ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(
      Exception ex, HttpServletRequest request) {

    log.error("Unhandled exception: {}", ex.getMessage(), ex);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", "服务器内部错误");
    body.put("path", request.getRequestURI());

    // Never expose details for 5xx errors in any environment
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
