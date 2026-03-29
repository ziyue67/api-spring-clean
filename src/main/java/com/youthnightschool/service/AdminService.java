package com.youthnightschool.service;

import com.youthnightschool.entity.Course;
import com.youthnightschool.entity.CourseSignup;
import com.youthnightschool.entity.SignLog;
import com.youthnightschool.entity.User;
import com.youthnightschool.repository.CourseRepository;
import com.youthnightschool.repository.CourseSignupRepository;
import com.youthnightschool.repository.SignLogRepository;
import com.youthnightschool.repository.UserRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Admin dashboard endpoints: stats, user list, course signups, sign trends.
 * Mirrors the NestJS AdminService.
 */
@Service
public class AdminService {

  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final CourseSignupRepository courseSignupRepository;
  private final SignLogRepository signLogRepository;

  public AdminService(
      UserRepository userRepository,
      CourseRepository courseRepository,
      CourseSignupRepository courseSignupRepository,
      SignLogRepository signLogRepository) {
    this.userRepository = userRepository;
    this.courseRepository = courseRepository;
    this.courseSignupRepository = courseSignupRepository;
    this.signLogRepository = signLogRepository;
  }

  /**
   * Returns overview stats: user count, course count, signup counts, recent users, top courses.
   */
  public Map<String, Object> getOverview() {
    long totalUsers = userRepository.count();
    long totalCourses = courseRepository.count();
    long totalSignups = courseSignupRepository.countByStatus("confirmed");
    long totalSignLogs = signLogRepository.count();
    long waitlistedSignups = courseSignupRepository.countByStatus("waitlisted");

    List<User> recentUsers = userRepository.findAll(
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createTime"))).getContent();

    List<Map<String, Object>> recentUsersList = recentUsers.stream()
        .map(u -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("id", u.getId());
          map.put("nickName", u.getNickName());
          map.put("avatarUrl", u.getAvatarUrl());
          map.put("phone", u.getPhone());
          map.put("points", u.getPoints());
          map.put("createTime", u.getCreateTime());
          map.put("lastLoginTime", u.getLastLoginTime());
          return map;
        })
        .toList();

    List<Course> topCourses = courseRepository.findAll(
        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();

    List<Map<String, Object>> topCoursesList = topCourses.stream()
        .map(c -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("id", c.getId());
          map.put("title", c.getTitle());
          map.put("college", c.getCollege());
          map.put("teacher", c.getTeacher());
          map.put("month", c.getMonth());
          map.put("maxSeats", c.getMaxSeats());
          map.put("status", c.getStatus());
          map.put("signupCount", c.getSignups() != null ? c.getSignups().size() : 0);
          return map;
        })
        .toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("totalUsers", totalUsers);
    result.put("totalCourses", totalCourses);
    result.put("totalConfirmedSignups", totalSignups);
    result.put("totalSignLogs", totalSignLogs);
    result.put("waitlistedSignups", waitlistedSignups);
    result.put("recentUsers", recentUsersList);
    result.put("topCourses", topCoursesList);
    return result;
  }

  /**
   * Returns per-course stats with confirmed/waitlisted counts and fill rate.
   */
  public List<Map<String, Object>> getCourseStats() {
    List<Course> courses = courseRepository.findAll(
        Sort.by(Sort.Direction.DESC, "createdAt"));

    List<Map<String, Object>> stats = new ArrayList<>();
    for (Course course : courses) {
      long confirmed = courseSignupRepository.countByCourseIdAndStatus(
          course.getId(), "confirmed");
      long waitlisted = courseSignupRepository.countByCourseIdAndStatus(
          course.getId(), "waitlisted");
      int fillRate = course.getMaxSeats() > 0
          ? Math.round((confirmed * 100f) / course.getMaxSeats())
          : 0;

      Map<String, Object> map = new LinkedHashMap<>();
      map.put("id", course.getId());
      map.put("title", course.getTitle());
      map.put("college", course.getCollege());
      map.put("teacher", course.getTeacher());
      map.put("month", course.getMonth());
      map.put("week", course.getWeek());
      map.put("timeStart", course.getTimeStart());
      map.put("timeEnd", course.getTimeEnd());
      map.put("maxSeats", course.getMaxSeats());
      map.put("status", course.getStatus());
      map.put("difficulty", course.getDifficulty());
      map.put("audience", course.getAudience());
      map.put("signupCount", course.getSignups() != null ? course.getSignups().size() : 0);
      map.put("confirmedCount", confirmed);
      map.put("waitlistedCount", waitlisted);
      map.put("fillRate", fillRate);
      stats.add(map);
    }
    return stats;
  }

  /**
   * Returns daily sign counts for the last N days.
   */
  public List<Map<String, Object>> getSignTrends(Integer days) {
    int effectiveDays = days != null ? Math.max(7, Math.min(days, 90)) : 30;
    Instant since = Instant.now().minus(java.time.Duration.ofDays(effectiveDays));

    List<SignLog> logs = signLogRepository.findByCreatedAtAfter(since);

    Map<String, Integer> dailyCount = new LinkedHashMap<>();
    DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    for (SignLog log : logs) {
      String dateStr = fmt.format(log.getCreatedAt());
      dailyCount.merge(dateStr, 1, Integer::sum);
    }

    return dailyCount.entrySet().stream()
        .map(entry -> Map.<String, Object>of("date", entry.getKey(), "count", entry.getValue()))
        .sorted((a, b) -> ((String) a.get("date")).compareTo((String) b.get("date")))
        .toList();
  }

  /**
   * Paginated user list with signup and sign counts. Optional keyword search.
   */
  public Map<String, Object> getUserList(Integer page, Integer pageSize, String keyword) {
    int effectivePage = page != null ? Math.max(1, page) : 1;
    int effectivePageSize = pageSize != null ? Math.max(1, Math.min(pageSize, 100)) : 20;

    Page<User> userPage;
    if (keyword != null && !keyword.isBlank()) {
      // Search by nickName, phone, or openid using database query
      userPage = userRepository.searchByKeyword(
          keyword,
          PageRequest.of(effectivePage - 1, effectivePageSize, Sort.by(Sort.Direction.DESC, "createTime")));
      long total = userPage.getTotalElements();

      List<Map<String, Object>> data = userPage.getContent().stream()
          .map(this::mapUserForList)
          .toList();

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("data", data);
      result.put("total", total);
      result.put("page", effectivePage);
      result.put("pageSize", effectivePageSize);
      result.put("totalPages", userPage.getTotalPages());
      return result;
    }

    userPage = userRepository.findAll(
        PageRequest.of(effectivePage - 1, effectivePageSize, Sort.by(Sort.Direction.DESC, "createTime")));
    long total = userPage.getTotalElements();

    List<Map<String, Object>> data = userPage.getContent().stream()
        .map(this::mapUserForList)
        .toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("data", data);
    result.put("total", total);
    result.put("page", effectivePage);
    result.put("pageSize", effectivePageSize);
    result.put("totalPages", (int) Math.ceil((double) total / effectivePageSize));
    return result;
  }

  private Map<String, Object> mapUserForList(User u) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", u.getId());
    map.put("openid", u.getOpenid());
    map.put("nickName", u.getNickName());
    map.put("avatarUrl", u.getAvatarUrl());
    map.put("phone", u.getPhone());
    map.put("points", u.getPoints());
    map.put("roles", u.getRolesList());
    map.put("createTime", u.getCreateTime());
    map.put("lastLoginTime", u.getLastLoginTime());
    map.put("signupCount", u.getCourseSignups() != null ? u.getCourseSignups().size() : 0);
    map.put("signCount", u.getSignLogs() != null ? u.getSignLogs().size() : 0);
    return map;
  }

  /**
   * Returns signup list for a course with user details.
   */
  public List<Map<String, Object>> getCourseSignupList(Integer courseId) {
    List<CourseSignup> all = new ArrayList<>(courseSignupRepository.findByCourseId(courseId));

    all.sort((a, b) -> {
      int cmp = a.getStatus().compareTo(b.getStatus());
      if (cmp != 0) return cmp;
      return a.getCreatedAt().compareTo(b.getCreatedAt());
    });

    return all.stream()
        .map(s -> {
          User user = s.getUser();
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("id", s.getId());
          map.put("status", s.getStatus());
          map.put("createdAt", s.getCreatedAt());
          map.put("userId", user != null ? user.getId() : s.getUserId());
          map.put("nickName", user != null && user.getNickName() != null ? user.getNickName() : "未知用户");
          map.put("avatarUrl", user != null && user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
          map.put("phone", user != null && user.getPhone() != null ? user.getPhone() : "");
          return map;
        })
        .toList();
  }

  /**
   * Returns course counts grouped by college.
   */
  public List<Map<String, Object>> getCollegeStats() {
    List<Course> allCourses = courseRepository.findAll();

    Map<String, Long> grouped = allCourses.stream()
        .collect(java.util.stream.Collectors.groupingBy(
            Course::getCollege,
            java.util.stream.Collectors.counting()));

    return grouped.entrySet().stream()
        .map(entry -> {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("college", entry.getKey());
          map.put("courseCount", entry.getValue());
          return map;
        })
        .toList();
  }
}
