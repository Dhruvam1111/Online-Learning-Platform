package com.olp.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for instructor grade override with mandatory justification (FR-I32).
 */
public class GradeOverrideRequest {

    @NotNull(message = "Updated marks cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Updated marks cannot be negative")
    @JsonAlias({ "newMarks", "awardedMarks", "marks", "score" })
    private BigDecimal updatedMarks;

    @NotBlank(message = "Justification is mandatory for grade overrides")
    @JsonAlias({ "reason", "comment", "note", "overrideReason" })
    private String justification;

    private Boolean publish = false;

    private Integer answerId;

    private Integer attemptId;

    public GradeOverrideRequest() {}

    public GradeOverrideRequest(BigDecimal updatedMarks, String justification) {
        this.updatedMarks = updatedMarks;
        this.justification = justification;
    }

    public GradeOverrideRequest(BigDecimal updatedMarks, String justification, Boolean publish) {
        this.updatedMarks = updatedMarks;
        this.justification = justification;
        this.publish = publish;
    }

    public BigDecimal getUpdatedMarks() {
        return updatedMarks;
    }

    public void setUpdatedMarks(BigDecimal updatedMarks) {
        this.updatedMarks = updatedMarks;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }
}
