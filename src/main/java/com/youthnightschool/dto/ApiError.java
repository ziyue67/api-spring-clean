package com.youthnightschool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized error response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    boolean success,
    String message,
    String path,
    String error
) {
    public ApiError(String message, String path, String error) {
        this(false, message, path, error);
    }
}
