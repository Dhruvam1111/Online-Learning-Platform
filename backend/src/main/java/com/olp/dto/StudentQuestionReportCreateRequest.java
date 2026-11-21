package com.olp.dto;

import com.olp.entity.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Student-facing question report request (FR-S41, FR-S42). */
public record StudentQuestionReportCreateRequest(
        @NotNull AssessmentType assessmentType,
        @NotNull @Positive Integer assessmentId,
        @NotNull @Positive Integer questionId,
        @NotBlank String reason,
        @NotBlank @Size(max = 4000) String explanation,
        @NotNull @Positive Integer attemptId
) {}
