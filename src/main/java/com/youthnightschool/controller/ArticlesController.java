package com.youthnightschool.controller;

import com.youthnightschool.dto.PaginationQuery;
import com.youthnightschool.dto.UpsertArticleRequest;
import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.security.AdminOpenidChecker;
import com.youthnightschool.security.RequestUser;
import com.youthnightschool.service.ArticlesService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Article sync and listing endpoints.
 * Mirrors the NestJS ArticlesController.
 */
@RestController
@RequestMapping("/api/v1/articles")
public class ArticlesController {

  private final ArticlesService articlesService;
  private final AdminOpenidChecker adminOpenidChecker;

  public ArticlesController(ArticlesService articlesService, AdminOpenidChecker adminOpenidChecker) {
    this.articlesService = articlesService;
    this.adminOpenidChecker = adminOpenidChecker;
  }

  @GetMapping
  public Map<String, Object> listAll(PaginationQuery query) {
    return articlesService.listAll(query.limitOrDefault());
  }

  @GetMapping("/recent")
  public Map<String, Object> listRecent(PaginationQuery query) {
    return articlesService.listRecent(query.limitOrDefault());
  }

  @PostMapping("/sync")
  @RateLimit(limit = 5, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> sync(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody List<UpsertArticleRequest> payload) {

    if (user == null || !adminOpenidChecker.check(user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("success", false, "message", "没有权限访问", "path", "/api/v1/articles/sync"));
    }

    if (payload.size() > 100) {
      return ResponseEntity.badRequest()
          .body(Map.of("success", false, "message", "单次最多同步100条文章", "path", "/api/v1/articles/sync"));
    }

    return ResponseEntity.ok(articlesService.upsertMany(payload));
  }
}
