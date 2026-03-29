package com.youthnightschool.security;

import com.youthnightschool.config.AppProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Checks whether the authenticated user's openid is in the admin allowlist.
 * Mirrors the NestJS {@code AdminOpenidGuard}.
 */
@Component
public class AdminOpenidChecker {

  private final AppProperties appProperties;

  public AdminOpenidChecker(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  /**
   * Returns {@code true} if the user's openid is present in the configured
   * {@code app.admin.openids} list. Returns {@code false} if the list is empty
   * or the openid is not found.
   */
  public boolean check(RequestUser user) {
    if (user == null || user.openid() == null) {
      return false;
    }

    List<String> allowedOpenids = appProperties.getAdminOpenidList();
    if (allowedOpenids.isEmpty()) {
      return false;
    }

    return allowedOpenids.contains(user.openid());
  }
}
