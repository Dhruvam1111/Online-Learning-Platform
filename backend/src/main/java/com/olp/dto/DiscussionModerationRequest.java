package com.olp.dto;

public class DiscussionModerationRequest {

    private String action;
    private String reason;
    private String updatedTitle;
    private String updatedBody;
    private boolean escalateToAdmin;

    public DiscussionModerationRequest() {
    }

    public DiscussionModerationRequest(
            String action,
            String reason,
            String updatedTitle,
            String updatedBody,
            boolean escalateToAdmin) {
        this.action = action;
        this.reason = reason;
        this.updatedTitle = updatedTitle;
        this.updatedBody = updatedBody;
        this.escalateToAdmin = escalateToAdmin;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUpdatedTitle() {
        return updatedTitle;
    }

    public void setUpdatedTitle(String updatedTitle) {
        this.updatedTitle = updatedTitle;
    }

    public String getUpdatedBody() {
        return updatedBody;
    }

    public void setUpdatedBody(String updatedBody) {
        this.updatedBody = updatedBody;
    }

    public boolean isEscalateToAdmin() {
        return escalateToAdmin;
    }

    public void setEscalateToAdmin(boolean escalateToAdmin) {
        this.escalateToAdmin = escalateToAdmin;
    }
}
