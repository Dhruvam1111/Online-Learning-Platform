package com.olp.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentAssessmentPerformanceDto {

    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer assessmentId;
    private String assessmentType;
    private String assessmentTitle;
    private Double maxMarks;
    private Double passingMarks;
    private Double bestMarks;
    private Double bestPercentage;
    private Double latestMarks;
    private Double latestPercentage;
    private boolean passed;
    private int totalAttemptsCount;
    private LocalDateTime lastSubmittedAt;
    private List<StudentAssessmentAttemptDto> attempts = new ArrayList<>();

    public StudentAssessmentPerformanceDto() {
    }

    public StudentAssessmentPerformanceDto(
            Integer studentId,
            String studentName,
            String studentEmail,
            Integer assessmentId,
            String assessmentType,
            String assessmentTitle,
            Double maxMarks,
            Double passingMarks,
            Double bestMarks,
            Double bestPercentage,
            Double latestMarks,
            Double latestPercentage,
            boolean passed,
            int totalAttemptsCount,
            LocalDateTime lastSubmittedAt,
            List<StudentAssessmentAttemptDto> attempts) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.assessmentId = assessmentId;
        this.assessmentType = assessmentType;
        this.assessmentTitle = assessmentTitle;
        this.maxMarks = maxMarks;
        this.passingMarks = passingMarks;
        this.bestMarks = bestMarks;
        this.bestPercentage = bestPercentage;
        this.latestMarks = latestMarks;
        this.latestPercentage = latestPercentage;
        this.passed = passed;
        this.totalAttemptsCount = totalAttemptsCount;
        this.lastSubmittedAt = lastSubmittedAt;
        this.attempts = attempts != null ? attempts : new ArrayList<>();
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

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public void setAssessmentTitle(String assessmentTitle) {
        this.assessmentTitle = assessmentTitle;
    }

    public Double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(Double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public Double getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(Double passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Double getBestMarks() {
        return bestMarks;
    }

    public void setBestMarks(Double bestMarks) {
        this.bestMarks = bestMarks;
    }

    public Double getBestPercentage() {
        return bestPercentage;
    }

    public void setBestPercentage(Double bestPercentage) {
        this.bestPercentage = bestPercentage;
    }

    public Double getLatestMarks() {
        return latestMarks;
    }

    public void setLatestMarks(Double latestMarks) {
        this.latestMarks = latestMarks;
    }

    public Double getLatestPercentage() {
        return latestPercentage;
    }

    public void setLatestPercentage(Double latestPercentage) {
        this.latestPercentage = latestPercentage;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getTotalAttemptsCount() {
        return totalAttemptsCount;
    }

    public void setTotalAttemptsCount(int totalAttemptsCount) {
        this.totalAttemptsCount = totalAttemptsCount;
    }

    public LocalDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }

    public List<StudentAssessmentAttemptDto> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<StudentAssessmentAttemptDto> attempts) {
        this.attempts = attempts != null ? attempts : new ArrayList<>();
    }
}
