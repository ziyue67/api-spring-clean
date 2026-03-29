package com.youthnightschool.service;

import com.youthnightschool.entity.User;
import com.youthnightschool.repository.UserRepository;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User profile CRUD.
 * Mirrors the NestJS UsersService exactly, including input sanitization.
 */
@Service
public class UsersService {

  private final UserRepository userRepository;

  public UsersService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  // --- input sanitization helpers (package-private for CoursesService reuse) ---

  static String sanitizeNickName(String nickName) {
    if (nickName == null) {
      return "";
    }
    String sanitized = nickName
        .replace("<", "")
        .replace(">", "")
        .trim();
    return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
  }

  static String normalizeAvatarUrl(String avatarUrl) {
    if (avatarUrl == null) {
      return "";
    }
    String trimmed = avatarUrl.trim();
    if (trimmed.isEmpty()) {
      return "";
    }
    try {
      URI uri = new URI(trimmed);
      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        return "";
      }
      return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    } catch (Exception e) {
      return "";
    }
  }

  static String normalizePhone(String phone) {
    if (phone == null) {
      return "";
    }
    return phone.trim();
  }

  static boolean isValidPhoneNumber(String phone) {
    return phone != null && phone.matches("^1[3-9]\\d{9}$");
  }

  // --- UpsertResult ---

  public record UpsertResult(boolean created, User user) {}

  // --- public API ---

  @Transactional
  public UpsertResult upsertUser(String openid, String unionid, String nickName,
      String avatarUrl, String phone) {
    Optional<User> existingOpt = userRepository.findByOpenid(openid);
    Instant now = Instant.now();

    if (existingOpt.isPresent()) {
      User existing = existingOpt.get();
      if (unionid != null) {
        existing.setUnionid(unionid);
      }
      if (nickName != null && !nickName.isEmpty()) {
        existing.setNickName(sanitizeNickName(nickName));
      }
      if (avatarUrl != null && !avatarUrl.isEmpty()) {
        existing.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
      }
      if (phone != null && !phone.isEmpty()) {
        existing.setPhone(normalizePhone(phone));
      }
      existing.setLastLoginTime(now);
      return new UpsertResult(false, userRepository.save(existing));
    }

    User user = new User();
    user.setOpenid(openid);
    user.setUnionid(unionid);
    String sanitized = sanitizeNickName(nickName);
    user.setNickName(sanitized.isEmpty() ? "微信用户" : sanitized);
    user.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
    if (phone != null && !phone.isEmpty()) {
      user.setPhone(normalizePhone(phone));
    }
    user.setLastLoginTime(now);
    return new UpsertResult(true, userRepository.save(user));
  }

  public Optional<User> findById(Integer id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByOpenid(String openid) {
    return userRepository.findByOpenid(openid);
  }

  public Optional<User> findByPhone(String phone) {
    return userRepository.findByPhone(phone);
  }

  @Transactional
  public User updateUser(Integer userId, String nickName, String avatarUrl) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

    if (nickName != null) {
      user.setNickName(sanitizeNickName(nickName));
    }
    if (avatarUrl != null) {
      user.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
    }
    user.setLastLoginTime(Instant.now());
    return userRepository.save(user);
  }

  @Transactional
  public User updatePhone(Integer userId, String phone) {
    String trimmedPhone = normalizePhone(phone);

    if (trimmedPhone.isEmpty()) {
      throw new RuntimeException("请输入手机号");
    }

    if (!isValidPhoneNumber(trimmedPhone)) {
      throw new RuntimeException("手机号格式不正确");
    }

    if (userRepository.existsByPhoneAndIdNot(trimmedPhone, userId)) {
      throw new RuntimeException("该手机号已绑定其他账号，请勿直接合并账号");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));
    user.setPhone(trimmedPhone);
    user.setLastLoginTime(Instant.now());
    return userRepository.save(user);
  }
}
