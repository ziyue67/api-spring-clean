package com.youthnightschool.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * DTO for paginated list queries.
 * Mirrors NestJS PaginationQueryDto — limit defaults to 20.
 */
public record PaginationQuery(
    @Min(1)
    @Max(100)
    Integer limit
) {
    public int limitOrDefault() {
        return limit != null ? limit : 20;
    }
}
