package com.youthnightschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for searching courses by keyword.
 * Mirrors NestJS SearchCoursesQueryDto.
 */
public record SearchCoursesQuery(
    @NotBlank
    @Size(max = 100)
    String keyword,

    @Size(max = 255)
    String college
) {}
