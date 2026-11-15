package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an aggregated reported question inbox item (FR-I35 Step 52, FR-I36 Step 53).
 * Aggregates student reports per assessment question with report counters, escalation timers, and resolution auditing.
 */
@Entity
@Table(
        name = "reported_questions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reported_question_assessment",
                        columnNames = {"assessment_type", "assessment_id", "question_id"}
                )
        }
)
public class ReportedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 20)
    private AssessmentType assessmentType;

    @Column(name = "assessment_id", nullable = false)
    private Integer assessmentId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportedQuestionStatus status = ReportedQuestionStatus.PENDING;

    @Column(name = "latest_reason", length = 255)
    private String latestReason;

    @Column(name = "latest_explanation", columnDefinition = "TEXT")
    private String latestExplanation;

    @Column(name = "first_reported_at", nullable = false)
    private LocalDateTime firstReportedAt;

    @Column(name = "last_reported_at", nullable = false)
    private LocalDateTime lastReportedAt;

    @Column(name = "escalate_at")
    private LocalDateTime escalateAt;

    @Column(name = "escalated_to_admin", nullable = false)
    private Boolean escalatedToAdmin = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "resolution_action", length = 50)
    private String resolutionAction;

    @Column(name = "resolution_comment", columnDefinition = "TEXT")
    private String resolutionComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ReportedQuestion() {}

    public ReportedQuestion(AssessmentType assessmentType, Integer assessmentId, Integer questionId, Course course) {
        this.assessmentType = assessmentType;
        this.assessmentId = assessmentId;
        this.questionId = questionId;
        this.course = course;
        this.reportCount = 1;
        this.status = ReportedQuestionStatus.PENDING;
        this.escalatedToAdmin = false;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.firstReportedAt == null) {
            this.firstReportedAt = now;
        }
        if (this.lastReportedAt == null) {
            this.lastReportedAt = now;
        }
        if (this.status == null) {
            this.status = ReportedQuestionStatus.PENDING;
        }
        if (this.reportCount == null) {
            this.reportCount = 1;
        }
        if (this.escalatedToAdmin == null) {
            this.escalatedToAdmin = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(AssessmentType assessmentType) {
        this.assessmentType = assessmentType;
    }

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Integer getReportCount() {
        return reportCount;
    }

    public void setReportCount(Integer reportCount) {
        this.reportCount = reportCount;
    }

    public ReportedQuestionStatus getStatus() {
        return status;
    }

    public void setStatus(ReportedQuestionStatus status) {
        this.status = status;
    }

    public String getLatestReason() {
        return latestReason;
    }

    public void setLatestReason(String latestReason) {
        this.latestReason = latestReason;
    }

    public String getLatestExplanation() {
        return latestExplanation;
    }

    public void setLatestExplanation(String latestExplanation) {
        this.latestExplanation = latestExplanation;
    }

    public LocalDateTime getFirstReportedAt() {
        return firstReportedAt;
    }

    public void setFirstReportedAt(LocalDateTime firstReportedAt) {
        this.firstReportedAt = firstReportedAt;
    }

    public LocalDateTime getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(LocalDateTime lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public LocalDateTime getEscalateAt() {
        return escalateAt;
    }

    public void setEscalateAt(LocalDateTime escalateAt) {
        this.escalateAt = escalateAt;
    }

    public Boolean getEscalatedToAdmin() {
        return escalatedToAdmin;
    }

    public void setEscalatedToAdmin(Boolean escalatedToAdmin) {
        this.escalatedToAdmin = escalatedToAdmin;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public void setResolutionAction(String resolutionAction) {
        this.resolutionAction = resolutionAction;
    }

    public String getResolutionComment() {
        return resolutionComment;
    }

    public void setResolutionComment(String resolutionComment) {
        this.resolutionComment = resolutionComment;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(User resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
