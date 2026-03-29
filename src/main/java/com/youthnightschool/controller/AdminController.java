package com.youthnightschool.controller;

import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.security.AdminOpenidChecker;
import com.youthnightschool.security.RequestUser;
import com.youthnightschool.service.AdminService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin dashboard endpoints: stats, user list, course signups.
 * Mirrors the NestJS AdminController — all endpoints require JWT + AdminOpenid.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

  private final AdminService adminService;
  private final AdminOpenidChecker adminOpenidChecker;

  public AdminController(AdminService adminService, AdminOpenidChecker adminOpenidChecker) {
    this.adminService = adminService;
    this.adminOpenidChecker = adminOpenidChecker;
  }

  private ResponseEntity<Map<String, Object>> checkAdmin(RequestUser user, String path) {
    if (user == null || !adminOpenidChecker.check(user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("success", false, "message", "没有权限访问", "path", path));
    }
    return null;
  }

  @GetMapping("/stats/overview")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getOverview(
      @AuthenticationPrincipal RequestUser user) {
    String path = "/api/v1/admin/stats/overview";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    Map<String, Object> data = adminService.getOverview();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/courses")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getCourseStats(
      @AuthenticationPrincipal RequestUser user) {
    String path = "/api/v1/admin/stats/courses";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    var data = adminService.getCourseStats();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/sign-trends")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getSignTrends(
      @AuthenticationPrincipal RequestUser user,
      @RequestParam(required = false) Integer days) {
    String path = "/api/v1/admin/stats/sign-trends";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    int effectiveDays = days != null ? Math.max(7, Math.min(days, 90)) : 30;
    var data = adminService.getSignTrends(effectiveDays);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/stats/colleges")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getCollegeStats(
      @AuthenticationPrincipal RequestUser user) {
    String path = "/api/v1/admin/stats/colleges";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    var data = adminService.getCollegeStats();
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", data);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/users")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getUserList(
      @AuthenticationPrincipal RequestUser user,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize,
      @RequestParam(required = false) String keyword) {
    String path = "/api/v1/admin/users";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    Map<String, Object> result = adminService.getUserList(page, pageSize, keyword);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", result.get("data"));
    response.put("count", result.get("total"));
    response.put("page", result.get("page"));
    response.put("pageSize", result.get("pageSize"));
    return ResponseEntity.ok(response);
  }

  @GetMapping("/courses/{courseId}/signups")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> getCourseSignups(
      @AuthenticationPrincipal RequestUser user,
      @PathVariable Integer courseId) {
    String path = "/api/v1/admin/courses/" + courseId + "/signups";
    ResponseEntity<Map<String, Object>> denied = checkAdmin(user, path);
    if (denied != null) return denied;

    var data = adminService.getCourseSignupList(courseId);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", data);
    return ResponseEntity.ok(response);
  }
}
