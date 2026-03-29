package com.youthnightschool.controller;

import com.youthnightschool.dto.CancelSignupRequest;
import com.youthnightschool.dto.CourseSignupRequest;
import com.youthnightschool.dto.GetCourseMonthsQuery;
import com.youthnightschool.dto.GetCoursesQuery;
import com.youthnightschool.dto.SearchCoursesQuery;
import com.youthnightschool.dto.SyncCourseRequest;
import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.security.AdminOpenidChecker;
import com.youthnightschool.security.RequestUser;
import com.youthnightschool.service.CoursesService;
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
 * Course catalog, signup/cancel, and sync endpoints.
 * Mirrors the NestJS CoursesController.
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CoursesController {

  private final CoursesService coursesService;
  private final AdminOpenidChecker adminOpenidChecker;

  public CoursesController(CoursesService coursesService, AdminOpenidChecker adminOpenidChecker) {
    this.coursesService = coursesService;
    this.adminOpenidChecker = adminOpenidChecker;
  }

  @GetMapping("/months")
  public Map<String, Object> getMonths(GetCourseMonthsQuery query) {
    return coursesService.getMonths(query.college());
  }

  @GetMapping
  public Map<String, Object> list(GetCoursesQuery query) {
    return coursesService.list(query.college(), query.month());
  }

  @GetMapping("/search")
  @RateLimit(limit = 30, ttlSeconds = 60)
  public Map<String, Object> search(SearchCoursesQuery query) {
    return coursesService.search(query.keyword(), query.college());
  }

  @GetMapping("/signup-list")
  public Map<String, Object> signupList(@AuthenticationPrincipal RequestUser user) {
    return coursesService.getSignupList(user.userId());
  }

  @PostMapping("/signup")
  @RateLimit(limit = 10, ttlSeconds = 60)
  public Map<String, Object> signup(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody CourseSignupRequest payload) {
    return coursesService.signup(user.userId(), payload);
  }

  @PostMapping("/cancel-signup")
  @RateLimit(limit = 10, ttlSeconds = 60)
  public Map<String, Object> cancelSignup(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody CancelSignupRequest payload) {
    return coursesService.cancelSignup(user.userId(), payload);
  }

  @PostMapping("/sync")
  @RateLimit(limit = 5, ttlSeconds = 60)
  public ResponseEntity<Map<String, Object>> sync(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody List<SyncCourseRequest> payload) {

    if (user == null || !adminOpenidChecker.check(user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("success", false, "message", "没有权限访问", "path", "/api/v1/courses/sync"));
    }

    if (payload.size() > 200) {
      return ResponseEntity.badRequest()
          .body(Map.of("success", false, "message", "单次最多同步200门课程", "path", "/api/v1/courses/sync"));
    }

    return ResponseEntity.ok(coursesService.sync(payload));
  }
}
