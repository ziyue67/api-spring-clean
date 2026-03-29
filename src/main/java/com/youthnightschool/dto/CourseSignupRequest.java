package com.youthnightschool.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for signing up for a course.
 * Mirrors NestJS CourseSignupDto — courseId is validated if legacyId is null,
 * and legacyId is validated if courseId is null.
 * Both fields are nullable; cross-field validation is performed in the service layer.
 */
public record CourseSignupRequest(
    Integer courseId,

    @Size(max = 64)
    String legacyId
) {}
