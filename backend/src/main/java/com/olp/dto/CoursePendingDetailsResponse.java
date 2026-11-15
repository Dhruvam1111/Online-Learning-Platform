package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed breakdown containing counts and lists of ungraded descriptive answers and assignments for a course (FR-I33, Step 50).
 */
public class CoursePendingDetailsResponse {

    private Integer courseId;
    private String courseTitle;
    private long totalPending;
    private long descriptivePending;
    private long assignmentPending;
    private List<DescriptiveSubmissionResponse> descriptiveSubmissions = new ArrayList<>();
    private List<AssignmentSubmissionResponse> assignmentSubmissions = new ArrayList<>();

    public CoursePendingDetailsResponse() {
    }

    public CoursePendingDetailsResponse(Integer courseId,
                                        String courseTitle,
                                        long totalPending,
                                        long descriptivePending,
                                        long assignmentPending,
                                        List<DescriptiveSubmissionResponse> descriptiveSubmissions,
                                        List<AssignmentSubmissionResponse> assignmentSubmissions) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalPending = totalPending;
        this.descriptivePending = descriptivePending;
        this.assignmentPending = assignmentPending;
        this.descriptiveSubmissions = descriptiveSubmissions != null ? descriptiveSubmissions : new ArrayList<>();
        this.assignmentSubmissions = assignmentSubmissions != null ? assignmentSubmissions : new ArrayList<>();
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

    public List<DescriptiveSubmissionResponse> getDescriptiveSubmissions() {
        return descriptiveSubmissions;
    }

    public void setDescriptiveSubmissions(List<DescriptiveSubmissionResponse> descriptiveSubmissions) {
        this.descriptiveSubmissions = descriptiveSubmissions;
    }

    public List<AssignmentSubmissionResponse> getAssignmentSubmissions() {
        return assignmentSubmissions;
    }

    public void setAssignmentSubmissions(List<AssignmentSubmissionResponse> assignmentSubmissions) {
        this.assignmentSubmissions = assignmentSubmissions;
    }
}
