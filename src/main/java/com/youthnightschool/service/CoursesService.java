package com.youthnightschool.service;

import com.youthnightschool.dto.CancelSignupRequest;
import com.youthnightschool.dto.CourseSignupRequest;
import com.youthnightschool.dto.SyncCourseRequest;
import com.youthnightschool.entity.Course;
import com.youthnightschool.entity.CourseSignup;
import com.youthnightschool.entity.User;
import com.youthnightschool.repository.CourseRepository;
import com.youthnightschool.repository.CourseSignupRepository;
import com.youthnightschool.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Course catalog, signup/cancel with waitlist, and sync.
 * Mirrors the NestJS CoursesService (~970 lines) as closely as possible.
 */
@Service
public class CoursesService {

  // --- Constants ---

  private static final Map<String, String> COLLEGE_ALIAS = Map.of(
      "社会发展与公共教育学院", "社会发展学院"
  );

  private static final Map<String, String> COURSE_STATUS_ALIAS = Map.ofEntries(
      Map.entry("available", "available"),
      Map.entry("open", "available"),
      Map.entry("报名中", "available"),
      Map.entry("closed", "closed"),
      Map.entry("inactive", "closed"),
      Map.entry("archived", "closed"),
      Map.entry("已关闭", "closed"),
      Map.entry("draft", "draft"),
      Map.entry("unpublished", "draft"),
      Map.entry("未发布", "draft")
  );

  private static final int DEFAULT_MAX_SEATS = 30;
  private static final int SEARCH_RESULT_LIMIT = 50;
  private static final String DEFAULT_SIGNUP_STATUS = "confirmed";
  private static final String WAITLIST_SIGNUP_STATUS = "waitlisted";
  private static final String CONFIRMED_SIGNUP_STATUS = "confirmed";

  private static final int LIMIT_TITLE = 255;
  private static final int LIMIT_COLLEGE = 255;
  private static final int LIMIT_TEACHER = 100;
  private static final int LIMIT_LOCATION = 255;
  private static final int LIMIT_DESCRIPTION = 4000;
  private static final int LIMIT_NOTICE = 4000;
  private static final int LIMIT_MATERIALS = 4000;
  private static final int LIMIT_DIFFICULTY = 64;
  private static final int LIMIT_AUDIENCE = 255;
  private static final int LIMIT_DURATION = 64;
  private static final int LIMIT_FEE = 64;
  private static final int LIMIT_WEEK = 64;
  private static final int LIMIT_TIME = 32;
  private static final int LIMIT_TAGS = 32;
  private static final int LIMIT_COVER_IMAGE = 500;

  private final CourseRepository courseRepository;
  private final CourseSignupRepository courseSignupRepository;
  private final UserRepository userRepository;

  public CoursesService(
      CourseRepository courseRepository,
      CourseSignupRepository courseSignupRepository,
      UserRepository userRepository) {
    this.courseRepository = courseRepository;
    this.courseSignupRepository = courseSignupRepository;
    this.userRepository = userRepository;
  }

  // --- text sanitization helpers ---

  private String sanitizeCourseText(Object value, int maxLength) {
    String text = normalizeTextInput(value);
    String sanitized = text
        .replaceAll("\\p{Cc}", " ")
        .replaceAll("[\\p{Cf}\\u3164\\uFFA0]", "")
        .replace("<", "").replace(">", "")
        .replaceAll("\\s+", " ")
        .trim();
    return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
  }

  private String sanitizeCourseText(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String result = value
        .replaceAll("\\p{Cc}", " ")
        .replaceAll("[\\p{Cf}\\u3164\\uFFA0]", "")
        .replace("<", "").replace(">", "")
        .replaceAll("\\s+", " ")
        .trim();
    return result.length() > maxLength ? result.substring(0, maxLength) : result;
  }

  private String normalizeTextInput(Object value) {
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    return "";
  }

  private String sanitizeCourseUrl(Object value) {
    String sanitized = sanitizeCourseText(value, LIMIT_COVER_IMAGE);
    if (sanitized.isEmpty()) {
      return "";
    }
    if (sanitized.toLowerCase().startsWith("http://") || sanitized.toLowerCase().startsWith("https://")) {
      return sanitized;
    }
    return "";
  }

  // --- normalization helpers ---

  private int normalizeMaxSeats(Integer maxSeats) {
    if (maxSeats != null && maxSeats > 0) {
      return maxSeats;
    }
    return DEFAULT_MAX_SEATS;
  }

  String normalizeCollegeName(String college) {
    if (college == null) {
      return null;
    }
    return COLLEGE_ALIAS.getOrDefault(college, college);
  }

  String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      return "available";
    }
    String trimmed = status.trim();
    return COURSE_STATUS_ALIAS.getOrDefault(trimmed, "available");
  }

  private boolean canSignup(String status) {
    return "available".equals(normalizeStatus(status));
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }
    return tags.stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  private String serializeTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return "";
    }
    return tags.stream()
        .map(tag -> sanitizeCourseText(tag, LIMIT_TAGS))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(","));
  }

  // --- payload builders ---

  private Map<String, Object> buildCoursePayload(Course course, long signupCount) {
    int maxSeats = normalizeMaxSeats(course.getMaxSeats());
    int remainingSeats = Math.max(0, maxSeats - (int) signupCount);
    String normalizedStatus = normalizeStatus(course.getStatus());

    String timeField = (sanitizeCourseText(course.getWeek(), LIMIT_WEEK) + " "
        + sanitizeCourseText(course.getTimeStart(), LIMIT_TIME) + "-"
        + sanitizeCourseText(course.getTimeEnd(), LIMIT_TIME)).trim();

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", course.getLegacyId() != null ? course.getLegacyId() : course.getId());
    payload.put("name", sanitizeCourseText(course.getTitle(), LIMIT_TITLE));
    payload.put("college", sanitizeCourseText(course.getCollege(), LIMIT_COLLEGE));
    payload.put("time", timeField);
    payload.put("teacher", sanitizeCourseText(course.getTeacher(), LIMIT_TEACHER));
    payload.put("location", sanitizeCourseText(course.getLocation(), LIMIT_LOCATION));
    payload.put("description", sanitizeCourseText(course.getDescription(), LIMIT_DESCRIPTION));
    payload.put("coverImage", sanitizeCourseUrl(course.getCoverImage()));
    payload.put("difficulty", sanitizeCourseText(course.getDifficulty(), LIMIT_DIFFICULTY));
    payload.put("audience", sanitizeCourseText(course.getAudience(), LIMIT_AUDIENCE));
    payload.put("duration", sanitizeCourseText(course.getDuration(), LIMIT_DURATION));
    payload.put("fee", sanitizeCourseText(course.getFee(), LIMIT_FEE));
    payload.put("notice", sanitizeCourseText(course.getNotice(), LIMIT_NOTICE));
    payload.put("materials", sanitizeCourseText(course.getMaterials(), LIMIT_MATERIALS));
    payload.put("tags", normalizeTags(parseTags(course.getTags())));
    payload.put("month", course.getMonth());
    payload.put("status", normalizedStatus);
    payload.put("signupCount", signupCount);
    payload.put("maxSeats", maxSeats);
    payload.put("remainingSeats", remainingSeats);
    payload.put("isFull", remainingSeats == 0);
    payload.putAll(buildSignupWindowPayload(course.getSignupStartAt(), course.getSignupEndAt()));
    return payload;
  }

  private Map<String, Object> buildCapacityPayload(long signupCount, Integer maxSeats) {
    int normalizedMaxSeats = normalizeMaxSeats(maxSeats);
    int remainingSeats = Math.max(0, normalizedMaxSeats - (int) signupCount);
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("signupCount", signupCount);
    map.put("maxSeats", normalizedMaxSeats);
    map.put("remainingSeats", remainingSeats);
    map.put("isFull", remainingSeats == 0);
    return map;
  }

  private Map<String, Object> buildSignupStatePayload(String signupStatus) {
    String normalizedStatus = signupStatus != null ? signupStatus : DEFAULT_SIGNUP_STATUS;
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("signupStatus", normalizedStatus);
    map.put("isWaitlisted", WAITLIST_SIGNUP_STATUS.equals(normalizedStatus));
    return map;
  }

  private Map<String, Object> buildSignupWindowPayload(Instant startAt, Instant endAt) {
    return buildSignupWindowPayload(startAt, endAt, Instant.now());
  }

  private Map<String, Object> buildSignupWindowPayload(Instant startAt, Instant endAt, Instant now) {
    long nowMs = now.toEpochMilli();
    Long startMs = startAt != null ? startAt.toEpochMilli() : null;
    Long endMs = endAt != null ? endAt.toEpochMilli() : null;
    boolean isOpen = (startMs == null || nowMs >= startMs) && (endMs == null || nowMs <= endMs);

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("signupStartAt", startAt != null ? startAt.toString() : null);
    map.put("signupEndAt", endAt != null ? endAt.toString() : null);
    map.put("isSignupOpen", isOpen);
    return map;
  }

  private List<String> parseTags(String tagsStr) {
    if (tagsStr == null || tagsStr.isBlank()) {
      return List.of();
    }
    return List.of(tagsStr.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  // --- waitlist helpers ---

  private Integer getWaitlistPosition(Integer courseId, Integer userId, String signupStatus) {
    if (!WAITLIST_SIGNUP_STATUS.equals(signupStatus)) {
      return null;
    }
    List<CourseSignup> waitlisted = courseSignupRepository
        .findByCourseIdInAndStatus(List.of(courseId), WAITLIST_SIGNUP_STATUS);

    // Sort by createdAt asc, id asc (repository query should handle this, but ensure it)
    waitlisted.sort((a, b) -> {
      int cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
      if (cmp != 0) return cmp;
      return a.getId().compareTo(b.getId());
    });

    for (int i = 0; i < waitlisted.size(); i++) {
      if (waitlisted.get(i).getUser().getId().equals(userId)) {
        return i + 1;
      }
    }
    return null;
  }

  private Map<Integer, Map<Integer, Integer>> getWaitlistPositions(List<Integer> courseIds) {
    List<Integer> uniqueIds = courseIds.stream().distinct().toList();
    if (uniqueIds.isEmpty()) {
      return Map.of();
    }

    List<CourseSignup> waitlisted = courseSignupRepository
        .findByCourseIdInAndStatus(uniqueIds, WAITLIST_SIGNUP_STATUS);

    // Sort by courseId asc, createdAt asc, id asc
    waitlisted.sort((a, b) -> {
      int cmp = a.getCourse().getId().compareTo(b.getCourse().getId());
      if (cmp != 0) return cmp;
      cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
      if (cmp != 0) return cmp;
      return a.getId().compareTo(b.getId());
    });

    Map<Integer, Map<Integer, Integer>> positions = new LinkedHashMap<>();
    for (CourseSignup signup : waitlisted) {
      int cId = signup.getCourse().getId();
      int uId = signup.getUser().getId();
      positions.computeIfAbsent(cId, k -> new LinkedHashMap<>());
      Map<Integer, Integer> courseMap = positions.get(cId);
      courseMap.put(uId, courseMap.size() + 1);
    }
    return positions;
  }

  // --- resolve course ---

  private Course resolveCourse(Integer courseId, String legacyId) {
    String trimmedLegacyId = legacyId != null ? legacyId.trim() : "";
    if (courseId != null) {
      return courseRepository.findById(courseId).orElse(null);
    }
    if (!trimmedLegacyId.isEmpty()) {
      return courseRepository.findByLegacyId(trimmedLegacyId).orElse(null);
    }
    return null;
  }

  // --- public API ---

  /**
   * Get distinct months for a college.
   */
  public Map<String, Object> getMonths(String college) {
    String normalizedCollege = normalizeCollegeName(college);
    List<Integer> months = courseRepository.findDistinctMonthsByCollege(normalizedCollege);
    return Map.of("success", true, "months", months);
  }

  /**
   * List courses by college and month, ordered by week ASC, timeStart ASC.
   */
  public Map<String, Object> list(String college, Integer month) {
    String normalizedCollege = normalizeCollegeName(college);
    List<Course> rows = courseRepository.findByCollegeAndMonth(
        normalizedCollege, month,
        PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "week", "timeStart"))
    ).getContent();

    List<Map<String, Object>> courses = new ArrayList<>();
    for (Course course : rows) {
      long signupCount = courseSignupRepository.countByCourseIdAndStatus(
          course.getId(), CONFIRMED_SIGNUP_STATUS);
      Map<String, Object> payload = buildCoursePayload(course, signupCount);
      // Strip extra fields not needed in list view
      courses.add(payload);
    }

    return Map.of("success", true, "courses", courses);
  }

  /**
   * Search courses by keyword, optional college filter. Max 50 results.
   */
  public Map<String, Object> search(String keyword, String college) {
    String trimmedKeyword = keyword != null ? keyword.trim() : "";
    if (trimmedKeyword.isEmpty()) {
      return Map.of("success", true, "results", List.of());
    }

    List<Course> rows;
    if (college != null && !college.isBlank()) {
      String normalizedCollege = normalizeCollegeName(college);
      rows = courseRepository.findByTitleContainingAndCollege(
          trimmedKeyword, normalizedCollege,
          PageRequest.of(0, SEARCH_RESULT_LIMIT,
              Sort.by(Sort.Direction.ASC, "month", "week", "timeStart"))
      ).getContent();
    } else {
      rows = courseRepository.findByTitleContaining(
          trimmedKeyword,
          PageRequest.of(0, SEARCH_RESULT_LIMIT,
              Sort.by(Sort.Direction.ASC, "month", "week", "timeStart"))
      ).getContent();
    }

    List<Map<String, Object>> results = new ArrayList<>();
    for (Course course : rows) {
      long signupCount = courseSignupRepository.countByCourseIdAndStatus(
          course.getId(), CONFIRMED_SIGNUP_STATUS);
      results.add(buildCoursePayload(course, signupCount));
    }

    return Map.of("success", true, "results", results);
  }

  /**
   * Get all signups for a user with course details and waitlist positions.
   */
  public Map<String, Object> getSignupList(Integer userId) {
    List<CourseSignup> rows = courseSignupRepository.findByUserIdOrderByCreatedAtDesc(userId);

    // Collect course IDs for waitlisted items
    List<Integer> waitlistedCourseIds = rows.stream()
        .filter(s -> WAITLIST_SIGNUP_STATUS.equals(s.getStatus()))
        .map(s -> s.getCourse().getId())
        .toList();

    Map<Integer, Map<Integer, Integer>> waitlistPositions = getWaitlistPositions(waitlistedCourseIds);

    List<Map<String, Object>> courses = new ArrayList<>();
    for (CourseSignup signup : rows) {
      Course course = signup.getCourse();
      long signupCount = courseSignupRepository.countByCourseIdAndStatus(
          course.getId(), CONFIRMED_SIGNUP_STATUS);
      Map<String, Object> coursePayload = buildCoursePayload(course, signupCount);

      Map<String, Object> item = new LinkedHashMap<>(coursePayload);
      Integer waitlistPos = null;
      if (WAITLIST_SIGNUP_STATUS.equals(signup.getStatus())) {
        Map<Integer, Integer> courseMap = waitlistPositions.get(course.getId());
        waitlistPos = courseMap != null ? courseMap.get(userId) : null;
      }
      item.put("waitlistPosition", waitlistPos);
      item.putAll(buildSignupStatePayload(signup.getStatus()));
      item.put("signedAt", signup.getCreatedAt().toString());
      courses.add(item);
    }

    return Map.of("success", true, "courses", courses);
  }

  /**
   * Sign up for a course. Handles waitlisting when course is full.
   */
  @Transactional
  public Map<String, Object> signup(Integer userId, CourseSignupRequest request) {
    if (userId == null) {
      return Map.of("success", false, "message", "缺少用户信息");
    }

    String legacyId = request.legacyId() != null ? request.legacyId().trim() : "";
    Course course = resolveCourse(request.courseId(), legacyId);

    if (course == null) {
      return Map.of("success", false, "message", "课程不存在");
    }

    // Check existing signup
    Optional<CourseSignup> existingOpt = courseSignupRepository
        .findByCourseIdAndUserId(course.getId(), userId);

    int normalizedMaxSeats = normalizeMaxSeats(course.getMaxSeats());
    long preSignupCount = courseSignupRepository.countByCourseIdAndStatus(
        course.getId(), CONFIRMED_SIGNUP_STATUS);

    // Check course status
    if (!canSignup(course.getStatus())) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("courseId", course.getId());
      data.put("legacyId", course.getLegacyId());
      data.putAll(buildCapacityPayload(preSignupCount, normalizedMaxSeats));
      data.putAll(buildSignupStatePayload(null));
      data.putAll(buildSignupWindowPayload(course.getSignupStartAt(), course.getSignupEndAt()));
      return Map.of("success", false, "message", "当前课程暂不可报名", "data", data);
    }

    // Check signup window
    Map<String, Object> signupWindow = buildSignupWindowPayload(
        course.getSignupStartAt(), course.getSignupEndAt());
    if (!(boolean) signupWindow.get("isSignupOpen")) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("courseId", course.getId());
      data.put("legacyId", course.getLegacyId());
      data.putAll(buildCapacityPayload(preSignupCount, normalizedMaxSeats));
      data.putAll(buildSignupStatePayload(null));
      data.putAll(signupWindow);
      return Map.of("success", false, "message", "当前不在报名时间内", "data", data);
    }

    // Already signed up?
    if (existingOpt.isPresent()) {
      CourseSignup existing = existingOpt.get();
      Integer waitlistPosition = getWaitlistPosition(course.getId(), userId, existing.getStatus());

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("courseId", course.getId());
      data.put("legacyId", course.getLegacyId());
      data.putAll(buildCapacityPayload(preSignupCount, normalizedMaxSeats));
      data.putAll(buildSignupStatePayload(existing.getStatus()));
      data.put("waitlistPosition", waitlistPosition);

      String message = WAITLIST_SIGNUP_STATUS.equals(existing.getStatus())
          ? "已在候补名单中" : "已报名该课程";
      return Map.of("success", true, "message", message, "data", data);
    }

    // New signup — lock the course row (pessimistic lock)
    courseRepository.findAndLockById(course.getId());

    long confirmedCount = courseSignupRepository.countByCourseIdAndStatus(
        course.getId(), CONFIRMED_SIGNUP_STATUS);
    String nextStatus = confirmedCount >= normalizedMaxSeats
        ? WAITLIST_SIGNUP_STATUS : CONFIRMED_SIGNUP_STATUS;

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    CourseSignup newSignup = new CourseSignup();
    newSignup.setCourse(course);
    newSignup.setUser(user);
    newSignup.setStatus(nextStatus);
    courseSignupRepository.save(newSignup);

    Integer waitlistPosition = getWaitlistPosition(course.getId(), userId, nextStatus);
    long latestSignupCount = courseSignupRepository.countByCourseIdAndStatus(
        course.getId(), CONFIRMED_SIGNUP_STATUS);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("courseId", course.getId());
    data.put("legacyId", course.getLegacyId());
    data.putAll(buildCapacityPayload(latestSignupCount, normalizedMaxSeats));
    data.putAll(buildSignupStatePayload(nextStatus));
    data.put("waitlistPosition", waitlistPosition);

    String message = WAITLIST_SIGNUP_STATUS.equals(nextStatus)
        ? "课程已满，已加入候补" : "报名成功";
    return Map.of("success", true, "message", message, "data", data);
  }

  /**
   * Cancel a course signup. Promotes first waitlisted user if a confirmed slot is freed.
   */
  @Transactional
  public Map<String, Object> cancelSignup(Integer userId, CancelSignupRequest request) {
    if (userId == null) {
      return Map.of("success", false, "message", "缺少用户信息");
    }

    String legacyId = request.legacyId() != null ? request.legacyId().trim() : "";
    Course course = resolveCourse(request.courseId(), legacyId);

    if (course == null) {
      return Map.of("success", false, "message", "课程不存在");
    }

    Optional<CourseSignup> existingOpt = courseSignupRepository
        .findByCourseIdAndUserId(course.getId(), userId);

    if (existingOpt.isEmpty()) {
      return Map.of("success", false, "message", "未找到报名记录");
    }

    CourseSignup existing = existingOpt.get();

    // Lock the course row for pessimistic write
    courseRepository.findAndLockById(course.getId());

    // Delete the signup
    courseSignupRepository.delete(existing);
    courseSignupRepository.flush();

    // If cancelled was confirmed, promote first waitlisted user
    if (CONFIRMED_SIGNUP_STATUS.equals(existing.getStatus())) {
      Optional<CourseSignup> nextWaitlisted = courseSignupRepository
          .findFirstByCourseIdAndStatusOrderByCreatedAtAscIdAsc(
              course.getId(), WAITLIST_SIGNUP_STATUS);
      if (nextWaitlisted.isPresent()) {
        CourseSignup toPromote = nextWaitlisted.get();
        toPromote.setStatus(CONFIRMED_SIGNUP_STATUS);
        courseSignupRepository.save(toPromote);
      }
    }

    long signupCount = courseSignupRepository.countByCourseIdAndStatus(
        course.getId(), CONFIRMED_SIGNUP_STATUS);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("courseId", course.getId());
    data.put("legacyId", course.getLegacyId());
    data.putAll(buildCapacityPayload(signupCount, course.getMaxSeats()));
    data.putAll(buildSignupStatePayload(null));

    return Map.of("success", true, "message", "已取消报名", "data", data);
  }

  /**
   * Batch sync courses. Max 200 per batch.
   */
  @Transactional
  public Map<String, Object> sync(List<SyncCourseRequest> payload) {
    List<SyncCourseRequest> normalized = payload.stream()
        .filter(item -> item.title() != null && !item.title().isBlank()
            && item.college() != null && !item.college().isBlank()
            && item.month() != null
            && item.week() != null && !item.week().isBlank()
            && item.timeStart() != null && !item.timeStart().isBlank()
            && item.timeEnd() != null && !item.timeEnd().isBlank())
        .limit(200)
        .toList();

    List<String> errors = new ArrayList<>();

    for (SyncCourseRequest course : normalized) {
      try {
        String title = sanitizeCourseText(course.title(), LIMIT_TITLE);
        String college = normalizeCollegeName(sanitizeCourseText(course.college(), LIMIT_COLLEGE));
        String teacher = sanitizeCourseText(course.teacher(), LIMIT_TEACHER);
        String location = sanitizeCourseText(course.location(), LIMIT_LOCATION);
        String description = sanitizeCourseText(course.description(), LIMIT_DESCRIPTION);
        String coverImage = sanitizeCourseUrl(course.coverImage());
        String difficulty = sanitizeCourseText(course.difficulty(), LIMIT_DIFFICULTY);
        String audience = sanitizeCourseText(course.audience(), LIMIT_AUDIENCE);
        String duration = sanitizeCourseText(course.duration(), LIMIT_DURATION);
        String fee = sanitizeCourseText(course.fee(), LIMIT_FEE);
        String notice = sanitizeCourseText(course.notice(), LIMIT_NOTICE);
        String materials = sanitizeCourseText(course.materials(), LIMIT_MATERIALS);
        String tags = serializeTags(course.tags());
        int maxSeats = normalizeMaxSeats(course.maxSeats());
        String status = normalizeStatus(course.status());

        if (course.legacyId() != null && !course.legacyId().isBlank()) {
          Optional<Course> existingOpt = courseRepository.findByLegacyId(course.legacyId());
          if (existingOpt.isPresent()) {
            Course existing = existingOpt.get();
            existing.setTitle(title);
            existing.setCollege(college);
            existing.setTeacher(teacher);
            existing.setLocation(location);
            existing.setDescription(description);
            existing.setCoverImage(coverImage);
            existing.setDifficulty(difficulty);
            existing.setAudience(audience);
            existing.setDuration(duration);
            existing.setFee(fee);
            existing.setNotice(notice);
            existing.setMaterials(materials);
            existing.setTags(tags);
            existing.setMonth(course.month());
            existing.setWeek(sanitizeCourseText(course.week(), LIMIT_WEEK));
            existing.setTimeStart(sanitizeCourseText(course.timeStart(), LIMIT_TIME));
            existing.setTimeEnd(sanitizeCourseText(course.timeEnd(), LIMIT_TIME));
            existing.setMaxSeats(maxSeats);
            existing.setSignupStartAt(course.signupStartAt());
            existing.setSignupEndAt(course.signupEndAt());
            existing.setStatus(status);
            courseRepository.save(existing);
            continue;
          }

          Course newCourse = new Course();
          newCourse.setLegacyId(course.legacyId());
          newCourse.setTitle(title);
          newCourse.setCollege(college);
          newCourse.setTeacher(teacher);
          newCourse.setLocation(location);
          newCourse.setDescription(description);
          newCourse.setCoverImage(coverImage);
          newCourse.setDifficulty(difficulty);
          newCourse.setAudience(audience);
          newCourse.setDuration(duration);
          newCourse.setFee(fee);
          newCourse.setNotice(notice);
          newCourse.setMaterials(materials);
          newCourse.setTags(tags);
          newCourse.setMonth(course.month());
          newCourse.setWeek(sanitizeCourseText(course.week(), LIMIT_WEEK));
          newCourse.setTimeStart(sanitizeCourseText(course.timeStart(), LIMIT_TIME));
          newCourse.setTimeEnd(sanitizeCourseText(course.timeEnd(), LIMIT_TIME));
          newCourse.setMaxSeats(maxSeats);
          newCourse.setSignupStartAt(course.signupStartAt());
          newCourse.setSignupEndAt(course.signupEndAt());
          newCourse.setStatus(status);
          courseRepository.save(newCourse);
          continue;
        }

        Course newCourse = new Course();
        newCourse.setTitle(title);
        newCourse.setCollege(college);
        newCourse.setTeacher(teacher);
        newCourse.setLocation(location);
        newCourse.setDescription(description);
        newCourse.setCoverImage(coverImage);
        newCourse.setDifficulty(difficulty);
        newCourse.setAudience(audience);
        newCourse.setDuration(duration);
        newCourse.setFee(fee);
        newCourse.setNotice(notice);
        newCourse.setMaterials(materials);
        newCourse.setTags(tags);
        newCourse.setMonth(course.month());
        newCourse.setWeek(sanitizeCourseText(course.week(), LIMIT_WEEK));
        newCourse.setTimeStart(sanitizeCourseText(course.timeStart(), LIMIT_TIME));
        newCourse.setTimeEnd(sanitizeCourseText(course.timeEnd(), LIMIT_TIME));
        newCourse.setMaxSeats(maxSeats);
        newCourse.setSignupStartAt(course.signupStartAt());
        newCourse.setSignupEndAt(course.signupEndAt());
        newCourse.setStatus(status);
        courseRepository.save(newCourse);
      } catch (Exception e) {
        errors.add("Course sync error: " + (course.legacyId() != null ? course.legacyId() : course.title())
            + " - " + e.getMessage());
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", true);
    result.put("count", normalized.size());
    result.put("message", "成功同步 " + normalized.size() + " 门课程");
    if (!errors.isEmpty()) {
      result.put("errors", errors);
    }
    return result;
  }
}
