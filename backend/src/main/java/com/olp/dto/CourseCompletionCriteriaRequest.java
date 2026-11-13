package com.olp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for configuring course certificate completion criteria (FR-I56, DR-03).
 */
public class CourseCompletionCriteriaRequest {

    @DecimalMin(value = "0.0", message = "Minimum lesson completion percentage must be at least 0%")
    @DecimalMax(value = "100.0", message = "Minimum lesson completion percentage cannot exceed 100%")
    private BigDecimal minLessonCompletionPercentage;

    @DecimalMin(value = "0.0", message = "Minimum quiz passing marks must be positive or zero")
    private BigDecimal minQuizPassingMarks;

    @DecimalMin(value = "0.0", message = "Minimum quiz passing percentage must be at least 0%")
    @DecimalMax(value = "100.0", message = "Minimum quiz passing percentage cannot exceed 100%")
    private BigDecimal minQuizPassingPercentage;

    private Boolean requireAllQuizzesPassed;

    private List<Integer> mandatoryLessonIds = new ArrayList<>();

    private List<Integer> mandatoryQuizIds = new ArrayList<>();

    public CourseCompletionCriteriaRequest() {}

    public CourseCompletionCriteriaRequest(BigDecimal minLessonCompletionPercentage,
                                           BigDecimal minQuizPassingMarks,
                                           Boolean requireAllQuizzesPassed,
                                           List<Integer> mandatoryLessonIds,
                                           List<Integer> mandatoryQuizIds) {
        this.minLessonCompletionPercentage = minLessonCompletionPercentage;
        this.minQuizPassingMarks = minQuizPassingMarks;
        this.requireAllQuizzesPassed = requireAllQuizzesPassed;
        this.mandatoryLessonIds = mandatoryLessonIds != null ? mandatoryLessonIds : new ArrayList<>();
        this.mandatoryQuizIds = mandatoryQuizIds != null ? mandatoryQuizIds : new ArrayList<>();
    }

    public BigDecimal getMinLessonCompletionPercentage() {
        return minLessonCompletionPercentage;
    }

    public void setMinLessonCompletionPercentage(BigDecimal minLessonCompletionPercentage) {
        this.minLessonCompletionPercentage = minLessonCompletionPercentage;
    }

    public BigDecimal getMinQuizPassingMarks() {
        return minQuizPassingMarks;
    }

    public void setMinQuizPassingMarks(BigDecimal minQuizPassingMarks) {
        this.minQuizPassingMarks = minQuizPassingMarks;
    }

    public BigDecimal getMinQuizPassingPercentage() {
        return minQuizPassingPercentage;
    }

    public void setMinQuizPassingPercentage(BigDecimal minQuizPassingPercentage) {
        this.minQuizPassingPercentage = minQuizPassingPercentage;
    }

    public Boolean getRequireAllQuizzesPassed() {
        return requireAllQuizzesPassed;
    }

    public void setRequireAllQuizzesPassed(Boolean requireAllQuizzesPassed) {
        this.requireAllQuizzesPassed = requireAllQuizzesPassed;
    }

    public List<Integer> getMandatoryLessonIds() {
        return mandatoryLessonIds;
    }

    public void setMandatoryLessonIds(List<Integer> mandatoryLessonIds) {
        this.mandatoryLessonIds = mandatoryLessonIds != null ? mandatoryLessonIds : new ArrayList<>();
    }

    public List<Integer> getMandatoryQuizIds() {
        return mandatoryQuizIds;
    }

    public void setMandatoryQuizIds(List<Integer> mandatoryQuizIds) {
        this.mandatoryQuizIds = mandatoryQuizIds != null ? mandatoryQuizIds : new ArrayList<>();
    }
}
