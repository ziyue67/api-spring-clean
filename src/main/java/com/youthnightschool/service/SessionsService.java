package com.youthnightschool.service;

import com.youthnightschool.entity.Session;
import com.youthnightschool.entity.User;
import com.youthnightschool.repository.SessionRepository;
import com.youthnightschool.repository.UserRepository;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages encrypted WeChat session keys with Redis caching.
 * Mirrors the NestJS SessionsService exactly.
 *
 * Cache key pattern: "session:{userId}", TTL 7 days (604800 seconds).
 */
@Service
public class SessionsService {

  private static final long SESSION_TTL_SECONDS = 7L * 24 * 60 * 60; // 604800

  private final SessionRepository sessionRepository;
  private final UserRepository userRepository;
  private final EncryptionService encryptionService;
  private final StringRedisTemplate redisTemplate;

  public SessionsService(
      SessionRepository sessionRepository,
      UserRepository userRepository,
      EncryptionService encryptionService,
      StringRedisTemplate redisTemplate) {
    this.sessionRepository = sessionRepository;
    this.userRepository = userRepository;
    this.encryptionService = encryptionService;
    this.redisTemplate = redisTemplate;
  }

  /**
   * Encrypts and persists the session key, also caches it in Redis.
   */
  @Transactional
  public void saveSession(Integer userId, String sessionKey) {
    String encrypted = encryptionService.encrypt(sessionKey);

    Optional<Session> existing = sessionRepository.findByUserId(userId);
    if (existing.isPresent()) {
      Session session = existing.get();
      session.setSessionKey(encrypted);
      sessionRepository.save(session);
    } else {
      User user = userRepository.getReferenceById(userId);
      Session session = new Session();
      session.setUser(user);
      session.setSessionKey(encrypted);
      sessionRepository.save(session);
    }

    redisTemplate.opsForValue().set("session:" + userId, encrypted,
        java.time.Duration.ofSeconds(SESSION_TTL_SECONDS));
  }

  /**
   * Retrieves and decrypts the session key.
   * Checks Redis first, falls back to database, and repopulates Redis on miss.
   */
  public Optional<String> getSessionKey(Integer userId) {
    // Check Redis first
    String cached = redisTemplate.opsForValue().get("session:" + userId);
    if (cached != null) {
      return Optional.of(encryptionService.decrypt(cached));
    }

    // Fallback to database
    Optional<Session> sessionOpt = sessionRepository.findByUserId(userId);
    if (sessionOpt.isEmpty()) {
      return Optional.empty();
    }

    Session session = sessionOpt.get();

    // Repopulate Redis
    redisTemplate.opsForValue().set("session:" + userId, session.getSessionKey(),
        java.time.Duration.ofSeconds(SESSION_TTL_SECONDS));

    return Optional.of(encryptionService.decrypt(session.getSessionKey()));
  }
}
