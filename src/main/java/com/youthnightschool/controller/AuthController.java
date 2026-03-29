package com.youthnightschool.controller;

import com.youthnightschool.dto.WechatLoginRequest;
import com.youthnightschool.interceptor.RateLimit;
import com.youthnightschool.service.AuthService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WeChat login endpoint.
 * Mirrors the NestJS AuthController — POST /api/v1/auth/wechat-login (public).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/wechat-login")
  @RateLimit(limit = 10, ttlSeconds = 60)
  public Map<String, Object> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
    AuthService.LoginResult result = authService.loginWithWechatCode(request);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("success", true);
    response.put("data", result.user());
    response.put("token", result.token());
    response.put("message", result.created() ? "注册并登录成功" : "登录成功");
    return response;
  }
}
