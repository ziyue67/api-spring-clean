package com.youthnightschool.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate limiter interceptor using Bucket4j in-memory buckets.
 * Per-IP+endpoint granularity with {@link RateLimit} annotation support.
 * Default limit (no annotation): 60 requests per 60 seconds.
 */
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RateLimiterInterceptor.class);
  private static final int DEFAULT_LIMIT = 60;
  private static final int DEFAULT_TTL_SECONDS = 60;

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

    int limit = DEFAULT_LIMIT;
    int ttlSeconds = DEFAULT_TTL_SECONDS;
    if (rateLimit != null) {
      limit = rateLimit.limit();
      ttlSeconds = rateLimit.ttlSeconds();
    }

    String clientIp = getClientIp(request);
    String endpoint = request.getRequestURI();
    String key = clientIp + ":" + endpoint;

    Bucket bucket = resolveBucket(key, limit, ttlSeconds);

    if (bucket.tryConsume(1)) {
      return true;
    }

    // Rate limited - return 429 with JSON body
    log.warn("Rate limit exceeded for key: {}", key);
    response.setStatus(429);
    response.setContentType("application/json;charset=UTF-8");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("message", "请求过于频繁，请稍后再试");
    body.put("path", endpoint);

    response.getWriter().write(objectMapper.writeValueAsString(body));
    return false;
  }

  private Bucket resolveBucket(String key, int limit, int ttlSeconds) {
    return buckets.computeIfAbsent(key, k -> {
      Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofSeconds(ttlSeconds)));
      return Bucket.builder().addLimit(bandwidth).build();
    });
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      return ip.split(",")[0].trim();
    }
    ip = request.getHeader("X-Real-IP");
    if (ip != null && !ip.isBlank()) {
      return ip.trim();
    }
    return request.getRemoteAddr();
  }
}
