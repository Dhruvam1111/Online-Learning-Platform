package com.olp.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO used by admins when approving or rejecting a course association request (FR-I58).
 */
public class CourseAssociationReviewRequest {

    @Size(max = 1000, message = "Admin notes must be 1000 characters or fewer")
    private String adminNotes;

    @Size(max = 1000, message = "Reason must be 1000 characters or fewer")
    private String reason;

    public CourseAssociationReviewRequest() {
    }

    public CourseAssociationReviewRequest(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String resolveNotes() {
        if (adminNotes != null && !adminNotes.trim().isEmpty()) {
            return adminNotes.trim();
        }
        if (reason != null && !reason.trim().isEmpty()) {
            return reason.trim();
        }
        return null;
    }
}

