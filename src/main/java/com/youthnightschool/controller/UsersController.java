package com.youthnightschool.controller;

import com.youthnightschool.config.AppProperties;
import com.youthnightschool.dto.UpdatePhoneRequest;
import com.youthnightschool.dto.UpdateUserRequest;
import com.youthnightschool.entity.User;
import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.security.RequestUser;
import com.youthnightschool.service.UsersService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints.
 * Mirrors the NestJS UsersController — all endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

  private final UsersService usersService;
  private final AppProperties appProperties;

  public UsersController(UsersService usersService, AppProperties appProperties) {
    this.usersService = usersService;
    this.appProperties = appProperties;
  }

  @GetMapping("/me")
  public Map<String, Object> getMe(@AuthenticationPrincipal RequestUser user) {
    Optional<User> data = usersService.findById(user.userId());
    if (data.isPresent()) {
      Map<String, Object> userMap = mapUser(data.get());
      return Map.of("success", true, "data", userMap);
    }
    return Map.of("success", false, "data", Map.of(), "error", "用户不存在");
  }

  @PatchMapping("/me")
  @RateLimit(limit = 6, ttlSeconds = 60)
  public Map<String, Object> updateMe(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody UpdateUserRequest payload) {
    User updated = usersService.updateUser(user.userId(), payload.nickName(), payload.avatarUrl());
    Map<String, Object> userMap = mapUser(updated);
    return Map.of("success", true, "data", userMap, "message", "用户信息更新成功");
  }

  @PatchMapping("/me/phone")
  @RateLimit(limit = 3, ttlSeconds = 60)
  public Map<String, Object> updatePhone(
      @AuthenticationPrincipal RequestUser user,
      @RequestBody UpdatePhoneRequest payload) {
    User updated = usersService.updatePhone(user.userId(), payload.phone());
    Map<String, Object> userMap = mapUser(updated);
    return Map.of("success", true, "data", userMap, "message", "手机号绑定成功");
  }

  @GetMapping("/find-by-phone")
  @PreAuthorize("hasRole('admin') or hasRole('super_admin')")
  @RateLimit(limit = 10, ttlSeconds = 60)
  public Map<String, Object> findByPhone(
      @AuthenticationPrincipal RequestUser currentUser,
      @RequestParam(required = false) String phone) {

    boolean isPhoneLookupEnabled = appProperties.getPhoneLookup().isEnabled();

    if (!isPhoneLookupEnabled) {
      return Map.of("success", false, "error", "该接口未启用");
    }

    if (phone == null || phone.isBlank()) {
      return Map.of("success", false, "error", "手机号参数缺失");
    }

    Optional<User> data = usersService.findByPhone(phone);

    if (data.isPresent()) {
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("success", true);
      response.put("data", Map.of("exists", true));
      response.put("error", null);
      return response;
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", false);
    response.put("data", null);
    response.put("error", "用户不存在");
    return response;
  }

  private Map<String, Object> mapUser(User u) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", u.getId());
    map.put("openid", u.getOpenid());
    map.put("unionid", u.getUnionid());
    map.put("nickName", u.getNickName());
    map.put("avatarUrl", u.getAvatarUrl());
    map.put("phone", u.getPhone());
    map.put("roles", u.getRolesList());
    map.put("points", u.getPoints());
    map.put("createTime", u.getCreateTime());
    map.put("lastLoginTime", u.getLastLoginTime());
    return map;
  }
}
