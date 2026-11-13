package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed result of evaluating a student against course certificate completion criteria (FR-I56, FR-S77, DR-03).
 */
public class CompletionEvaluationResult {

    private boolean eligible;
    private Integer courseId;
    private String courseTitle;
    private Integer studentId;
    private String studentName;

    // Lesson criteria metrics
    private Double lessonCompletionPercentage;
    private Double requiredLessonCompletionPercentage;
    private int totalLessons;
    private int completedLessons;
    private boolean lessonCriteriaSatisfied;

    // Quiz criteria metrics
    private int totalQuizzes;
    private int passedQuizzes;
    private BigDecimal minQuizPassingMarks;
    private boolean quizCriteriaSatisfied;

    // Mandatory items metrics
    private boolean mandatoryLessonsSatisfied = true;
    private List<Integer> missingMandatoryLessonIds = new ArrayList<>();
    private List<String> missingMandatoryLessonTitles = new ArrayList<>();

    private boolean mandatoryQuizzesSatisfied = true;
    private List<Integer> missingMandatoryQuizIds = new ArrayList<>();
    private List<String> missingMandatoryQuizTitles = new ArrayList<>();

    // Unmet criteria explanations
    private List<String> unmetCriteriaReasons = new ArrayList<>();

    // Certificate metadata
    private boolean criteriaLocked;
    private boolean certificateIssued;
    private Integer certificateId;
    private String certificateCode;
    private LocalDateTime certificateIssuedAt;
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public CompletionEvaluationResult() {}

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
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

    public Double getLessonCompletionPercentage() {
        return lessonCompletionPercentage;
    }

    public void setLessonCompletionPercentage(Double lessonCompletionPercentage) {
        this.lessonCompletionPercentage = lessonCompletionPercentage;
    }

    public Double getRequiredLessonCompletionPercentage() {
        return requiredLessonCompletionPercentage;
    }

    public void setRequiredLessonCompletionPercentage(Double requiredLessonCompletionPercentage) {
        this.requiredLessonCompletionPercentage = requiredLessonCompletionPercentage;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public boolean isLessonCriteriaSatisfied() {
        return lessonCriteriaSatisfied;
    }

    public void setLessonCriteriaSatisfied(boolean lessonCriteriaSatisfied) {
        this.lessonCriteriaSatisfied = lessonCriteriaSatisfied;
    }

    public int getTotalQuizzes() {
        return totalQuizzes;
    }

    public void setTotalQuizzes(int totalQuizzes) {
        this.totalQuizzes = totalQuizzes;
    }

    public int getPassedQuizzes() {
        return passedQuizzes;
    }

    public void setPassedQuizzes(int passedQuizzes) {
        this.passedQuizzes = passedQuizzes;
    }

    public BigDecimal getMinQuizPassingMarks() {
        return minQuizPassingMarks;
    }

    public void setMinQuizPassingMarks(BigDecimal minQuizPassingMarks) {
        this.minQuizPassingMarks = minQuizPassingMarks;
    }

    public boolean isQuizCriteriaSatisfied() {
        return quizCriteriaSatisfied;
    }

    public void setQuizCriteriaSatisfied(boolean quizCriteriaSatisfied) {
        this.quizCriteriaSatisfied = quizCriteriaSatisfied;
    }

    public boolean isMandatoryLessonsSatisfied() {
        return mandatoryLessonsSatisfied;
    }

    public void setMandatoryLessonsSatisfied(boolean mandatoryLessonsSatisfied) {
        this.mandatoryLessonsSatisfied = mandatoryLessonsSatisfied;
    }

    public List<Integer> getMissingMandatoryLessonIds() {
        return missingMandatoryLessonIds;
    }

    public void setMissingMandatoryLessonIds(List<Integer> missingMandatoryLessonIds) {
        this.missingMandatoryLessonIds = missingMandatoryLessonIds != null ? missingMandatoryLessonIds : new ArrayList<>();
    }

    public List<String> getMissingMandatoryLessonTitles() {
        return missingMandatoryLessonTitles;
    }

    public void setMissingMandatoryLessonTitles(List<String> missingMandatoryLessonTitles) {
        this.missingMandatoryLessonTitles = missingMandatoryLessonTitles != null ? missingMandatoryLessonTitles : new ArrayList<>();
    }

    public boolean isMandatoryQuizzesSatisfied() {
        return mandatoryQuizzesSatisfied;
    }

    public void setMandatoryQuizzesSatisfied(boolean mandatoryQuizzesSatisfied) {
        this.mandatoryQuizzesSatisfied = mandatoryQuizzesSatisfied;
    }

    public List<Integer> getMissingMandatoryQuizIds() {
        return missingMandatoryQuizIds;
    }

    public void setMissingMandatoryQuizIds(List<Integer> missingMandatoryQuizIds) {
        this.missingMandatoryQuizIds = missingMandatoryQuizIds != null ? missingMandatoryQuizIds : new ArrayList<>();
    }

    public List<String> getMissingMandatoryQuizTitles() {
        return missingMandatoryQuizTitles;
    }

    public void setMissingMandatoryQuizTitles(List<String> missingMandatoryQuizTitles) {
        this.missingMandatoryQuizTitles = missingMandatoryQuizTitles != null ? missingMandatoryQuizTitles : new ArrayList<>();
    }

    public List<String> getUnmetCriteriaReasons() {
        return unmetCriteriaReasons;
    }

    public void setUnmetCriteriaReasons(List<String> unmetCriteriaReasons) {
        this.unmetCriteriaReasons = unmetCriteriaReasons != null ? unmetCriteriaReasons : new ArrayList<>();
    }

    public void addUnmetReason(String reason) {
        if (reason != null && !reason.trim().isEmpty()) {
            this.unmetCriteriaReasons.add(reason.trim());
        }
    }

    public boolean isCriteriaLocked() {
        return criteriaLocked;
    }

    public void setCriteriaLocked(boolean criteriaLocked) {
        this.criteriaLocked = criteriaLocked;
    }

    public boolean isCertificateIssued() {
        return certificateIssued;
    }

    public void setCertificateIssued(boolean certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public LocalDateTime getCertificateIssuedAt() {
        return certificateIssuedAt;
    }

    public void setCertificateIssuedAt(LocalDateTime certificateIssuedAt) {
        this.certificateIssuedAt = certificateIssuedAt;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
