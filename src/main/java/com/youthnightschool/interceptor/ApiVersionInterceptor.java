package com.youthnightschool.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API version interceptor. Reads the {@code X-API-Version} header (default "1")
 * and stores it as a request attribute for downstream consumption.
 * Mirrors the NestJS header-based versioning.
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

  public static final String API_VERSION_ATTR = "apiVersion";
  private static final String VERSION_HEADER = "X-API-Version";
  private static final String DEFAULT_VERSION = "1";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String version = request.getHeader(VERSION_HEADER);
    if (version == null || version.isBlank()) {
      version = DEFAULT_VERSION;
    }
    request.setAttribute(API_VERSION_ATTR, version);
    return true;
  }
}
