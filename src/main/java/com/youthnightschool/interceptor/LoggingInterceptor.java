package com.youthnightschool.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logging interceptor that records request method, URI, and response status.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
  private static final String START_TIME_ATTR = "requestStartTime";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
    long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;
    if (ex != null) {
      log.error("{} {} {} {}ms - exception: {}", request.getMethod(), request.getRequestURI(),
          response.getStatus(), duration, ex.getMessage());
    } else {
      log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(),
          response.getStatus(), duration);
    }
  }
}
