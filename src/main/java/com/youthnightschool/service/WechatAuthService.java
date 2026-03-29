package com.youthnightschool.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.youthnightschool.config.AppProperties;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Exchanges WeChat login codes for openid / session_key / unionid.
 * Mirrors the NestJS WechatAuthService.
 */
@Service
public class WechatAuthService {

  private final String appId;
  private final String appSecret;
  private final RestClient restClient;

  public WechatAuthService(AppProperties appProperties, RestClient.Builder restClientBuilder) {
    this.appId = appProperties.getWechat().getAppId();
    this.appSecret = appProperties.getWechat().getAppSecret();
    this.restClient = restClientBuilder.build();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Code2SessionResponse(
      String openid,
      String session_key,
      String unionid,
      Integer errcode,
      String errmsg) {}

  public record WechatSession(String openid, String unionid, String sessionKey) {}

  public WechatSession exchangeCode(String code) {
    if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
      throw new RuntimeException("微信登录环境变量未配置");
    }

    if (code == null || code.startsWith("mock-")) {
      throw new IllegalArgumentException("请通过微信小程序重新发起登录");
    }

    URI uri = UriComponentsBuilder
        .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
        .queryParam("appid", appId)
        .queryParam("secret", appSecret)
        .queryParam("js_code", code)
        .queryParam("grant_type", "authorization_code")
        .build(true)
        .toUri();

    Code2SessionResponse result = restClient.get()
        .uri(uri)
        .retrieve()
        .body(Code2SessionResponse.class);

    if (result == null) {
      throw new RuntimeException("微信登录失败");
    }

    if (result.openid == null || result.openid.isEmpty()
        || result.session_key == null || result.session_key.isEmpty()
        || (result.errcode != null && result.errcode != 0)) {
      throw new RuntimeException(getErrorMessage(result));
    }

    return new WechatSession(result.openid, result.unionid, result.session_key);
  }

  private String getErrorMessage(Code2SessionResponse result) {
    if (result.errcode != null) {
      return result.errmsg != null && !result.errmsg.isEmpty()
          ? "微信登录失败：" + result.errmsg
          : "微信登录失败：errcode=" + result.errcode;
    }
    return "微信登录失败";
  }
}
