package com.youthnightschool.security;

import com.youthnightschool.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Signs and verifies JWT auth tokens using HS256 with a 7-day expiry.
 * Mirrors the NestJS {@code TokenService}.
 */
@Component
public class JwtTokenProvider {

  private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

  private final AppProperties appProperties;
  private final SecretKey signingKey;

  public JwtTokenProvider(AppProperties appProperties) {
    this.appProperties = appProperties;
    String secret = appProperties.getJwt().getSecret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("app.jwt.secret must be configured");
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Signs a new auth token for the given userId and optional unionid.
   * The subject ({@code sub}) claim is the numeric userId (as String per JWT spec).
   */
  public String signAuthToken(Integer userId, String unionid) {
    long now = System.currentTimeMillis();
    var builder =
        Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(new Date(now))
            .expiration(new Date(now + SEVEN_DAYS_MS))
            .signWith(signingKey);

    if (unionid != null && !unionid.isBlank()) {
      builder.claim("unionid", unionid);
    }

    return builder.compact();
  }

  /**
   * Verifies the token and returns the parsed claims.
   *
   * @throws JwtException if the token is expired, malformed, or has an invalid signature
   */
  public Claims verifyAuthToken(String token) throws JwtException {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * Extracts the userId (numeric sub claim) from verified claims.
   */
  public Integer getUserId(Claims claims) {
    String sub = claims.getSubject();
    if (sub == null) {
      return null;
    }
    return Integer.valueOf(sub);
  }

  /**
   * Extracts the unionid claim, may be null.
   */
  public String getUnionid(Claims claims) {
    return claims.get("unionid", String.class);
  }
}
