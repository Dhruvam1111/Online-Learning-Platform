package com.olp.dto;

import java.math.BigDecimal;

/**
 * Summary of a single student attempt modified during question correction (FR-I37, Step 54).
 */
public class AttemptCorrectionSummaryResponse {

    private Integer attemptId;
    private Integer studentId;
    private String studentName;
    private BigDecimal oldScore;
    private BigDecimal newScore;
    private boolean certificateEligible;

    public AttemptCorrectionSummaryResponse() {}

    public AttemptCorrectionSummaryResponse(Integer attemptId, Integer studentId, String studentName,
                                           BigDecimal oldScore, BigDecimal newScore, boolean certificateEligible) {
        this.attemptId = attemptId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.certificateEligible = certificateEligible;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public BigDecimal getOldScore() {
        return oldScore;
    }

    public void setOldScore(BigDecimal oldScore) {
        this.oldScore = oldScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public void setNewScore(BigDecimal newScore) {
        this.newScore = newScore;
    }

    public boolean isCertificateEligible() {
        return certificateEligible;
    }

    public void setCertificateEligible(boolean certificateEligible) {
        this.certificateEligible = certificateEligible;
    }
}
