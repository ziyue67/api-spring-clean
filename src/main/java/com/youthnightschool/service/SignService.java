package com.youthnightschool.service;

import com.youthnightschool.entity.SignLog;
import com.youthnightschool.entity.User;
import com.youthnightschool.repository.SignLogRepository;
import com.youthnightschool.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily check-in + points.
 * Mirrors the NestJS SignService exactly, including SERIALIZABLE isolation
 * and duplicate-sign handling.
 */
@Service
public class SignService {

  private static final int DEFAULT_POINTS = 10;

  private final UserRepository userRepository;
  private final SignLogRepository signLogRepository;

  public SignService(UserRepository userRepository, SignLogRepository signLogRepository) {
    this.userRepository = userRepository;
    this.signLogRepository = signLogRepository;
  }

  private LocalDate getToday() {
    return LocalDate.now(ZoneOffset.UTC);
  }

  private String normalizeSignDate(LocalDate date) {
    return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  /**
   * Returns the user's sign-in status: points, total signs, today's status, recent 6.
   */
  public Map<String, Object> getStatus(Integer userId) {
    User user = userRepository.findById(userId).orElse(null);

    if (user == null) {
      return Map.of(
          "success", false,
          "points", 0,
          "totalSigns", 0,
          "signedToday", false,
          "signRecord", List.of()
      );
    }

    LocalDate today = getToday();
    boolean signedToday = signLogRepository.existsByUserIdAndSignDate(userId, today);
    long totalSigns = signLogRepository.countByUserId(userId);
    List<SignLog> recentLogs = signLogRepository.findTop6ByUserIdOrderByCreatedAtDesc(userId);

    List<Map<String, Object>> signRecord = recentLogs.stream()
        .map(log -> {
          Map<String, Object> entry = new LinkedHashMap<>();
          entry.put("date", normalizeSignDate(log.getSignDate()));
          entry.put("points", log.getPoints());
          return entry;
        })
        .toList();

    return Map.of(
        "success", true,
        "points", user.getPoints(),
        "totalSigns", totalSigns,
        "signedToday", signedToday,
        "signRecord", signRecord
    );
  }

  /**
   * Performs a daily check-in for the user.
   * Uses SERIALIZABLE isolation to prevent race conditions.
   * Handles duplicate signs gracefully (same behavior as NestJS P2002 error code).
   */
  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Map<String, Object> sign(Integer userId) {
    return sign(userId, DEFAULT_POINTS);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Map<String, Object> sign(Integer userId, int points) {
    LocalDate today = getToday();

    try {
      // Check for existing sign today
      boolean alreadySigned = signLogRepository.existsByUserIdAndSignDate(userId, today);
      if (alreadySigned) {
        return Map.of(
            "success", false,
            "error", "今日已签到",
            "signed", true
        );
      }

      // Increment user points
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RuntimeException("用户不存在"));
      user.setPoints(user.getPoints() + points);
      user.setLastLoginTime(Instant.now());
      userRepository.save(user);

      // Create sign log
      SignLog signLog = new SignLog();
      signLog.setUser(user);
      signLog.setSignDate(today);
      signLog.setPoints(points);
      signLogRepository.save(signLog);

      return Map.of(
          "success", true,
          "points", points,
          "user", user
      );
    } catch (DataIntegrityViolationException e) {
      // Unique constraint violation — same as NestJS P2002
      return Map.of(
          "success", false,
          "error", "今日已签到",
          "signed", true
      );
    }
  }
}
