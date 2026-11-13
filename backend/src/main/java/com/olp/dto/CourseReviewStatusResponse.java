package com.olp.dto;

import com.olp.entity.Course;
import com.olp.entity.CourseStatus;
import com.olp.entity.CourseSubmission;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the review status of a course for instructors and admins (FR-I71).
 * Exposes review status (PENDING, APPROVED, REJECTED, CHANGES_REQUESTED),
 * admin review comments, rejection/change-request reasons, and resubmission eligibility.
 */
public class CourseReviewStatusResponse {

    private Integer courseId;
    private String courseTitle;
    private String status;
    private String reviewStatus;
    private String adminComments;
    private String rejectionReason;
    private String changeRequestReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private long submissionCount;
    private boolean canResubmit;
    private boolean canEdit;
    private List<CourseSubmissionResponse> submissionHistory = new ArrayList<>();

    public CourseReviewStatusResponse() {}

    public static String mapToReviewStatus(CourseStatus courseStatus) {
        if (courseStatus == null) {
            return "DRAFT";
        }
        return switch (courseStatus) {
            case PENDING_REVIEW -> "PENDING";
            case PUBLISHED -> "APPROVED";
            case REJECTED -> "REJECTED";
            case CHANGES_REQUESTED -> "CHANGES_REQUESTED";
            case DRAFT -> "DRAFT";
            case ARCHIVED -> "ARCHIVED";
        };
    }

    public static CourseReviewStatusResponse fromCourseAndSubmissions(
            Course course,
            CourseSubmission latestSubmission,
            long totalSubmissions,
            List<CourseSubmissionResponse> history) {

        if (course == null) return null;

        CourseReviewStatusResponse response = new CourseReviewStatusResponse();
        response.setCourseId(course.getId());
        response.setCourseTitle(course.getTitle());

        CourseStatus currentStatus = course.getStatus();
        String mappedReviewStatus = mapToReviewStatus(currentStatus);
        response.setStatus(mappedReviewStatus);
        response.setReviewStatus(mappedReviewStatus);

        // Map admin comments and reasons
        String adminComments = course.getReviewComments();
        String rejectionReason = course.getRejectionReason();
        String changeRequestReason = null;

        if (currentStatus == CourseStatus.CHANGES_REQUESTED) {
            changeRequestReason = (adminComments != null && !adminComments.isBlank())
                    ? adminComments
                    : rejectionReason;
        }

        // Fall back to latest submission review comments if not directly on course entity
        if ((adminComments == null || adminComments.isBlank()) && latestSubmission != null) {
            adminComments = latestSubmission.getReviewComments();
        }

        response.setAdminComments(adminComments);
        response.setRejectionReason(rejectionReason);
        response.setChangeRequestReason(changeRequestReason);

        if (latestSubmission != null) {
            response.setSubmittedAt(latestSubmission.getSubmittedAt());
            response.setReviewedAt(latestSubmission.getReviewedAt());
        }

        response.setSubmissionCount(totalSubmissions);
        // Only allow resubmission when status is CHANGES_REQUESTED
        response.setCanResubmit(currentStatus == CourseStatus.CHANGES_REQUESTED);
        // Allow editing when CHANGES_REQUESTED or DRAFT
        response.setCanEdit(currentStatus == CourseStatus.CHANGES_REQUESTED || currentStatus == CourseStatus.DRAFT);

        if (history != null) {
            response.setSubmissionHistory(history);
        }

        return response;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getAdminComments() {
        return adminComments;
    }

    public void setAdminComments(String adminComments) {
        this.adminComments = adminComments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getChangeRequestReason() {
        return changeRequestReason;
    }

    public void setChangeRequestReason(String changeRequestReason) {
        this.changeRequestReason = changeRequestReason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public long getSubmissionCount() {
        return submissionCount;
    }

    public void setSubmissionCount(long submissionCount) {
        this.submissionCount = submissionCount;
    }

    public boolean isCanResubmit() {
        return canResubmit;
    }

    public void setCanResubmit(boolean canResubmit) {
        this.canResubmit = canResubmit;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public List<CourseSubmissionResponse> getSubmissionHistory() {
        return submissionHistory;
    }

    public void setSubmissionHistory(List<CourseSubmissionResponse> submissionHistory) {
        this.submissionHistory = submissionHistory != null ? submissionHistory : new ArrayList<>();
    }
}

