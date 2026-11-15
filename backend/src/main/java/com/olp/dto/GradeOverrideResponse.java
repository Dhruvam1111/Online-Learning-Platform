package com.olp.dto;

import com.olp.entity.GradingResultVersion;
import com.olp.entity.QuizAttempt;
import com.olp.entity.QuizAttemptAnswer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returned after performing a grade override (FR-I32).
 */
public class GradeOverrideResponse {

    private Integer versionId;
    private Integer versionNumber;
    private Integer attemptId;
    private Integer answerId;
    private Integer questionId;
    private Integer studentId;
    private String studentName;
    private BigDecimal previousMarks;
    private BigDecimal newMarks;
    private BigDecimal maxMarks;
    private BigDecimal attemptTotalMarks;
    private String justification;
    private Integer instructorId;
    private String instructorName;
    private String statusBefore;
    private String statusAfter;
    private LocalDateTime timestamp;
    private String message;

    public GradeOverrideResponse() {}

    public static GradeOverrideResponse fromEntity(GradingResultVersion v, QuizAttempt attempt, QuizAttemptAnswer answer, String message) {
        GradeOverrideResponse res = new GradeOverrideResponse();
        if (v != null) {
            res.setVersionId(v.getId());
            res.setVersionNumber(v.getVersionNumber());
            res.setPreviousMarks(v.getPreviousMarks());
            res.setNewMarks(v.getNewMarks());
            res.setMaxMarks(v.getMaxMarks());
            res.setJustification(v.getJustification());
            if (v.getInstructor() != null) {
                res.setInstructorId(v.getInstructor().getId());
                res.setInstructorName(v.getInstructor().getName());
            }
            res.setStatusBefore(v.getStatusBefore());
            res.setStatusAfter(v.getStatusAfter());
            res.setTimestamp(v.getCreatedAt());
        }
        if (attempt != null) {
            res.setAttemptId(attempt.getId());
            res.setAttemptTotalMarks(attempt.getObtainedMarks());
            if (attempt.getStudent() != null) {
                res.setStudentId(attempt.getStudent().getId());
                res.setStudentName(attempt.getStudent().getName());
            }
        }
        if (answer != null) {
            res.setAnswerId(answer.getId());
            if (answer.getQuestion() != null) {
                res.setQuestionId(answer.getQuestion().getId());
            }
        }
        res.setMessage(message);
        return res;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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

    public BigDecimal getPreviousMarks() {
        return previousMarks;
    }

    public void setPreviousMarks(BigDecimal previousMarks) {
        this.previousMarks = previousMarks;
    }

    public BigDecimal getNewMarks() {
        return newMarks;
    }

    public void setNewMarks(BigDecimal newMarks) {
        this.newMarks = newMarks;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getAttemptTotalMarks() {
        return attemptTotalMarks;
    }

    public void setAttemptTotalMarks(BigDecimal attemptTotalMarks) {
        this.attemptTotalMarks = attemptTotalMarks;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public String getStatusBefore() {
        return statusBefore;
    }

    public void setStatusBefore(String statusBefore) {
        this.statusBefore = statusBefore;
    }

    public String getStatusAfter() {
        return statusAfter;
    }

    public void setStatusAfter(String statusAfter) {
        this.statusAfter = statusAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
