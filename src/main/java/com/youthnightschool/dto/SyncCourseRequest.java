package com.youthnightschool.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for syncing/creating a course.
 * Mirrors NestJS SyncCourseDto.
 */
public record SyncCourseRequest(
    @Size(max = 64)
    String legacyId,

    @NotBlank
    @Size(max = 255)
    String title,

    @NotBlank
    @Size(max = 255)
    String college,

    @Size(max = 100)
    String teacher,

    @Size(max = 255)
    String location,

    @Size(max = 4000)
    String description,

    @Size(max = 500)
    String coverImage,

    @Size(max = 64)
    String difficulty,

    @Size(max = 255)
    String audience,

    @Size(max = 64)
    String duration,

    @Size(max = 64)
    String fee,

    @Size(max = 4000)
    String notice,

    @Size(max = 4000)
    String materials,

    @Size(max = 10)
    List<@Size(max = 32) String> tags,

    @NotNull
    @Min(1)
    Integer month,

    @NotBlank
    @Size(max = 64)
    String week,

    @NotBlank
    @Size(max = 32)
    String timeStart,

    @NotBlank
    @Size(max = 32)
    String timeEnd,

    Instant signupStartAt,

    Instant signupEndAt,

    @Min(1)
    Integer maxSeats,

    String status
) {}
