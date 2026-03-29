package com.youthnightschool.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating user profile.
 * Mirrors NestJS UpdateUserDto — all fields optional.
 */
public record UpdateUserRequest(
    @Size(max = 100)
    String nickName,

    @Size(max = 500)
    String avatarUrl
) {}
