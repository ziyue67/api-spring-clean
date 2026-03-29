package com.youthnightschool.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for daily check-in.
 * Mirrors NestJS SignDto — noop field is optional and effectively unused.
 */
public record SignRequest(
    @Size(max = 1)
    String noop
) {}
