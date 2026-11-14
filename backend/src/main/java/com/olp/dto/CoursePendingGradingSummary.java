package com.olp.dto;

/**
 * Summary DTO of pending grading workload for an individual course (FR-I33, Step 50).
 */
public class CoursePendingGradingSummary {

    private Integer courseId;
    private String courseTitle;
    private long totalPending;
    private long descriptivePending;
    private long assignmentPending;

    public CoursePendingGradingSummary() {
    }

    public CoursePendingGradingSummary(Integer courseId,
                                       String courseTitle,
                                       long totalPending,
                                       long descriptivePending,
                                       long assignmentPending) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalPending = totalPending;
        this.descriptivePending = descriptivePending;
        this.assignmentPending = assignmentPending;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public long getTotalPending() {
        return totalPending;
    }

    public void setTotalPending(long totalPending) {
        this.totalPending = totalPending;
    }

    public long getDescriptivePending() {
        return descriptivePending;
    }

    public void setDescriptivePending(long descriptivePending) {
        this.descriptivePending = descriptivePending;
    }

    public long getAssignmentPending() {
        return assignmentPending;
    }

    public void setAssignmentPending(long assignmentPending) {
        this.assignmentPending = assignmentPending;
    }
}
