package com.youthnightschool.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for querying courses by college and month.
 * Mirrors NestJS GetCoursesQueryDto.
 */
public record GetCoursesQuery(
    @NotBlank
    @Size(max = 255)
    String college,

    @NotNull
    @Min(1)
    @Max(12)
    Integer month
) {}
