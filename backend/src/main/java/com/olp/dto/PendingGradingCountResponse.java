package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated badge count response for instructor dashboard (FR-I33, Step 50).
 */
public class PendingGradingCountResponse {

    private long totalPendingCount;
    private long descriptiveQuizPendingCount;
    private long assignmentPendingCount;
    private List<CoursePendingGradingSummary> courses = new ArrayList<>();

    public PendingGradingCountResponse() {
    }

    public PendingGradingCountResponse(long totalPendingCount,
                                       long descriptiveQuizPendingCount,
                                       long assignmentPendingCount,
                                       List<CoursePendingGradingSummary> courses) {
        this.totalPendingCount = totalPendingCount;
        this.descriptiveQuizPendingCount = descriptiveQuizPendingCount;
        this.assignmentPendingCount = assignmentPendingCount;
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    public long getTotalPendingCount() {
        return totalPendingCount;
    }

    public void setTotalPendingCount(long totalPendingCount) {
        this.totalPendingCount = totalPendingCount;
    }

    public long getDescriptiveQuizPendingCount() {
        return descriptiveQuizPendingCount;
    }

    public void setDescriptiveQuizPendingCount(long descriptiveQuizPendingCount) {
        this.descriptiveQuizPendingCount = descriptiveQuizPendingCount;
    }

    public long getAssignmentPendingCount() {
        return assignmentPendingCount;
    }

    public void setAssignmentPendingCount(long assignmentPendingCount) {
        this.assignmentPendingCount = assignmentPendingCount;
    }

    public List<CoursePendingGradingSummary> getCourses() {
        return courses;
    }

    public void setCourses(List<CoursePendingGradingSummary> courses) {
        this.courses = courses;
    }
}
