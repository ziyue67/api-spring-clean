package com.youthnightschool.security;

import com.youthnightschool.entity.User;
import java.security.Principal;

/**
 * Represents the authenticated user extracted from the JWT token and loaded
 * from the database. Mirrors the NestJS {@code RequestUser} type.
 */
public record RequestUser(String openid, Integer userId, String unionid)
    implements Principal {

  @Override
  public String getName() {
    return String.valueOf(userId);
  }

  /**
   * Creates a RequestUser from a JPA User entity and the unionid from the JWT claims.
   */
  public static RequestUser from(User entity) {
    return new RequestUser(entity.getOpenid(), entity.getId(), entity.getUnionid());
  }
}
