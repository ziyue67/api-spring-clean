package com.youthnightschool.service;

import com.youthnightschool.dto.WechatLoginRequest;
import com.youthnightschool.entity.User;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Orchestrates WeChat login: exchange code -> upsert user -> save session -> sign JWT.
 * Mirrors the NestJS AuthService.
 */
@Service
public class AuthService {

  private final UsersService usersService;
  private final SessionsService sessionsService;
  private final TokenService tokenService;
  private final WechatAuthService wechatAuthService;

  public AuthService(
      UsersService usersService,
      SessionsService sessionsService,
      TokenService tokenService,
      WechatAuthService wechatAuthService) {
    this.usersService = usersService;
    this.sessionsService = sessionsService;
    this.tokenService = tokenService;
    this.wechatAuthService = wechatAuthService;
  }

  /** Login result containing user data, JWT token, and whether this was a new registration. */
  public record LoginResult(boolean created, Map<String, Object> user, String token) {}

  /**
   * Full WeChat login flow.
   *
   * @return LoginResult with user map, token, and created flag
   */
  public LoginResult loginWithWechatCode(WechatLoginRequest request) {
    WechatAuthService.WechatSession session = wechatAuthService.exchangeCode(request.code());

    UsersService.UpsertResult result = usersService.upsertUser(
        session.openid(),
        session.unionid(),
        request.nickName(),
        request.avatarUrl(),
        null // phone not provided during WeChat login
    );

    User user = result.user();

    sessionsService.saveSession(user.getId(), session.sessionKey());

    String token = tokenService.signAuthToken(user.getId(), user.getUnionid());

    Map<String, Object> userData = new LinkedHashMap<>();
    userData.put("id", user.getId());
    userData.put("openid", user.getOpenid());
    userData.put("unionid", user.getUnionid());
    userData.put("nickName", user.getNickName());
    userData.put("avatarUrl", user.getAvatarUrl());
    userData.put("phone", user.getPhone());
    userData.put("roles", user.getRolesList());
    userData.put("points", user.getPoints());
    userData.put("createTime", user.getCreateTime());
    userData.put("lastLoginTime", user.getLastLoginTime());

    return new LoginResult(result.created(), userData, token);
  }
}
