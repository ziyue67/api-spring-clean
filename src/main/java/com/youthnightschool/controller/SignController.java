package com.youthnightschool.controller;

import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.security.RequestUser;
import com.youthnightschool.service.SignService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Daily check-in + points endpoints.
 * Mirrors the NestJS SignController — all endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/v1/sign")
public class SignController {

  private final SignService signService;

  public SignController(SignService signService) {
    this.signService = signService;
  }

  @GetMapping("/status")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public Map<String, Object> getStatus(@AuthenticationPrincipal RequestUser user) {
    return signService.getStatus(user.userId());
  }

  @PostMapping
  @RateLimit(limit = 10, ttlSeconds = 60)
  public Map<String, Object> sign(@AuthenticationPrincipal RequestUser user) {
    return signService.sign(user.userId(), 10);
  }
}
