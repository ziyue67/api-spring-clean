package com.youthnightschool.service;

import com.youthnightschool.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

/**
 * Delegates JWT sign/verify to JwtTokenProvider.
 * Mirrors the NestJS TokenService.
 */
@Service
public class TokenService {

  private final JwtTokenProvider jwtTokenProvider;

  public TokenService(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /**
   * Signs a new auth token with userId as subject and optional unionid claim.
   * 7-day expiry, HS256.
   */
  public String signAuthToken(Integer userId, String unionid) {
    return jwtTokenProvider.signAuthToken(userId, unionid);
  }

  /**
   * Verifies the token and returns the parsed claims.
   *
   * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
   */
  public Claims verifyAuthToken(String token) {
    return jwtTokenProvider.verifyAuthToken(token);
  }
}
