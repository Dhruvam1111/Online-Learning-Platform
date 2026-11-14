package com.olp.dto;

/**
 * Response DTO for content removal operations (FR-I16, Step 19).
 * Returns status on whether content was soft-deleted or hard-deleted,
 * along with student progress and course completion criteria warning flags.
 */
public class ContentRemovalResponse {

    private Integer id;
    private boolean deleted;
    private boolean softDeleted;
    private String message;
    private String warning;
    private boolean criteriaWarning;

    public ContentRemovalResponse() {}

    public ContentRemovalResponse(Integer id, boolean deleted, boolean softDeleted,
                                  String message, String warning, boolean criteriaWarning) {
        this.id = id;
        this.deleted = deleted;
        this.softDeleted = softDeleted;
        this.message = message;
        this.warning = warning;
        this.criteriaWarning = criteriaWarning;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public boolean isCriteriaWarning() {
        return criteriaWarning;
    }

    public void setCriteriaWarning(boolean criteriaWarning) {
        this.criteriaWarning = criteriaWarning;
    }
}
