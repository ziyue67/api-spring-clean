package com.youthnightschool.controller;

import com.youthnightschool.service.HealthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint.
 * Mirrors the NestJS HealthController — public, skip rate limit.
 */
@RestController
public class HealthController {

  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping("/api/v1/health")
  public Map<String, Object> getStatus() {
    return healthService.getStatus();
  }
}
