package com.youthnightschool.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Health check for DB and Redis.
 * Mirrors the NestJS HealthService.
 */
@Service
public class HealthService {

  private final JdbcTemplate jdbcTemplate;
  private final RedisConnectionFactory redisConnectionFactory;

  public HealthService(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    this.redisConnectionFactory = redisConnectionFactory;
  }

  /**
   * Returns health status: checks DB via SELECT 1 and Redis via PING.
   */
  public Map<String, Object> getStatus() {
    String dbStatus = "down";
    String redisStatus = "down";

    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      dbStatus = "up";
    } catch (Exception ignored) {
      // DB is down
    }

    try {
      String pong = redisConnectionFactory.getConnection().ping();
      redisStatus = "PONG".equalsIgnoreCase(pong) ? "up" : "down";
    } catch (Exception ignored) {
      // Redis is down
    }

    boolean isHealthy = "up".equals(dbStatus) && "up".equals(redisStatus);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("status", isHealthy ? "ok" : "error");
    result.put("timestamp", Instant.now().toString());
    result.put("db", dbStatus);
    result.put("redis", redisStatus);
    return result;
  }
}
