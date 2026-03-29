package com.youthnightschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for querying available course months.
 * Mirrors NestJS GetCourseMonthsQueryDto.
 */
public record GetCourseMonthsQuery(
    @NotBlank
    @Size(max = 255)
    String college
) {}
