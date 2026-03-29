package com.youthnightschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for updating user phone number.
 * Mirrors NestJS UpdatePhoneDto.
 */
public record UpdatePhoneRequest(
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    String phone
) {}
