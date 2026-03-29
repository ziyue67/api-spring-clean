package com.youthnightschool.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for cancelling a course signup.
 * Same shape as CourseSignupRequest — courseId validated if legacyId is null,
 * legacyId validated if courseId is null.
 * Both fields are nullable; cross-field validation is performed in the service layer.
 */
public record CancelSignupRequest(
    Integer courseId,

    @Size(max = 64)
    String legacyId
) {}
