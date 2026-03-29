package com.youthnightschool.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Root API endpoint.
 * Mirrors the NestJS AppController — returns service metadata at GET /api.
 */
@RestController
public class RootController {

  @GetMapping("/api")
  public Map<String, Object> getRoot() {
    return Map.of(
        "service", "qinghe-night-school-api",
        "status", "ok",
        "stage", "foundation"
    );
  }
}
