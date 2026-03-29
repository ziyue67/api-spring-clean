package com.youthnightschool.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private final Jwt jwt = new Jwt();
  private final Wechat wechat = new Wechat();
  private final Encryption encryption = new Encryption();
  private final Admin admin = new Admin();
  private final PhoneLookup phoneLookup = new PhoneLookup();
  private final Cors cors = new Cors();
  private final Perf perf = new Perf();
  private final Redis redis = new Redis();
  private long requestTimeoutMs = 30000;

  public Jwt getJwt() {
    return jwt;
  }

  public Wechat getWechat() {
    return wechat;
  }

  public Encryption getEncryption() {
    return encryption;
  }

  public Admin getAdmin() {
    return admin;
  }

  public PhoneLookup getPhoneLookup() {
    return phoneLookup;
  }

  public Cors getCors() {
    return cors;
  }

  public Perf getPerf() {
    return perf;
  }

  public Redis getRedis() {
    return redis;
  }

  public long getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public void setRequestTimeoutMs(long requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
  }

  /**
   * Returns the comma-separated admin openids as a trimmed, non-empty list.
   */
  public List<String> getAdminOpenidList() {
    return parseCommaList(admin.openids);
  }

  /**
   * Returns the comma-separated allowed CORS origins as a trimmed, non-empty list.
   */
  public List<String> getAllowedOriginList() {
    return parseCommaList(cors.allowedOrigins);
  }

  private static List<String> parseCommaList(String value) {
    if (value == null || value.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  // ---- nested groups ----

  public static class Jwt {
    private String secret;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }
  }

  public static class Wechat {
    private String appId;
    private String appSecret;

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getAppSecret() {
      return appSecret;
    }

    public void setAppSecret(String appSecret) {
      this.appSecret = appSecret;
    }
  }

  public static class Encryption {
    private String sessionKey;

    public String getSessionKey() {
      return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
      this.sessionKey = sessionKey;
    }
  }

  public static class Admin {
    private String openids;

    public String getOpenids() {
      return openids;
    }

    public void setOpenids(String openids) {
      this.openids = openids;
    }
  }

  public static class PhoneLookup {
    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class Cors {
    private String allowedOrigins;

    public String getAllowedOrigins() {
      return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
    }
  }

  public static class Perf {
    private long slowThresholdMs = 1000;
    private long verySlowThresholdMs = 3000;
    private boolean headersEnabled = false;
    private boolean metricsEnabled = false;

    public long getSlowThresholdMs() {
      return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
      this.slowThresholdMs = slowThresholdMs;
    }

    public long getVerySlowThresholdMs() {
      return verySlowThresholdMs;
    }

    public void setVerySlowThresholdMs(long verySlowThresholdMs) {
      this.verySlowThresholdMs = verySlowThresholdMs;
    }

    public boolean isHeadersEnabled() {
      return headersEnabled;
    }

    public void setHeadersEnabled(boolean headersEnabled) {
      this.headersEnabled = headersEnabled;
    }

    public boolean isMetricsEnabled() {
      return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
    }
  }

  public static class Redis {
    private boolean tls = false;
    private boolean tlsRejectUnauthorized = true;

    public boolean isTls() {
      return tls;
    }

    public void setTls(boolean tls) {
      this.tls = tls;
    }

    public boolean isTlsRejectUnauthorized() {
      return tlsRejectUnauthorized;
    }

    public void setTlsRejectUnauthorized(boolean tlsRejectUnauthorized) {
      this.tlsRejectUnauthorized = tlsRejectUnauthorized;
    }
  }
}
