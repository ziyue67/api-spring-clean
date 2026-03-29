package com.youthnightschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for WeChat login requests.
 * Mirrors NestJS WechatLoginDto.
 */
public record WechatLoginRequest(
    @NotBlank
    @Size(max = 512)
    String code,

    @Size(max = 100)
    String nickName,

    @Size(max = 500)
    String avatarUrl
) {}
