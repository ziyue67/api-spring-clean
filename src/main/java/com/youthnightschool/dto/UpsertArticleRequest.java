package com.youthnightschool.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for upserting an article.
 * Mirrors NestJS UpsertArticleDto.
 */
public record UpsertArticleRequest(
    @Size(max = 64)
    String legacyId,

    @NotBlank
    @Size(max = 255)
    String title,

    @NotBlank
    @Pattern(regexp = "^https?://.*", message = "link must be a valid URL")
    @Size(max = 500)
    String link,

    Instant publishTime
) {}
