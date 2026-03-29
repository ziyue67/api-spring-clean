package com.youthnightschool.interceptor;

import com.youthnightschool.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Performance interceptor that logs slow requests and optionally adds
 * performance headers to the response.
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);
  private static final String START_TIME_ATTR = "perfStartTime";

  private final AppProperties appProperties;

  public PerformanceInterceptor(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
    if (startTime == null) {
      return;
    }
    long duration = System.currentTimeMillis() - startTime;

    if (appProperties.getPerf().isHeadersEnabled()) {
      response.setHeader("X-Response-Time", duration + "ms");
    }

    long slowThreshold = appProperties.getPerf().getSlowThresholdMs();
    long verySlowThreshold = appProperties.getPerf().getVerySlowThresholdMs();

    if (duration >= verySlowThreshold) {
      log.warn("VERY SLOW REQUEST: {} {} took {}ms", request.getMethod(),
          request.getRequestURI(), duration);
    } else if (duration >= slowThreshold) {
      log.warn("Slow request: {} {} took {}ms", request.getMethod(),
          request.getRequestURI(), duration);
    }
  }
}
