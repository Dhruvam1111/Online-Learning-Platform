package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entity representing a student attempt on a scheduled examination (FR-I25,
 * Step 38).
 * Enforces server-authoritative timers (DR-ED06) to prevent client clock
 * tampering.
 */
@Entity
@Table(name = "exam_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_student_attempt", columnNames = { "exam_id", "student_id", "attempt_number" })
})
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "IN_PROGRESS";

    @Column(name = "total_marks", precision = 8, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "obtained_marks", precision = 8, scale = 2)
    private BigDecimal obtainedMarks;

    @Column(name = "set_label", length = 50)
    private String setLabel;

    @Column(name = "random_seed")
    private Long randomSeed;

    @Column(name = "question_order", columnDefinition = "TEXT")
    private String questionOrder;

    @Column(name = "option_shuffle_map", columnDefinition = "TEXT")
    private String optionShuffleMap;

    @Column(name = "consent_given", nullable = false)
    private Boolean consentGiven = false;

    @Column(name = "consent_given_at")
    private LocalDateTime consentGivenAt;

    @Column(name = "tab_switch_count", nullable = false)
    private Integer tabSwitchCount = 0;

    @Column(name = "fullscreen_exit_count", nullable = false)
    private Integer fullscreenExitCount = 0;

    @Column(name = "monitoring_flags", columnDefinition = "TEXT")
    private String monitoringFlags;

    @Column(name = "screen_share_consent_given", nullable = false)
    private Boolean screenShareConsentGiven = false;

    @Column(name = "screen_share_consent_at")
    private LocalDateTime screenShareConsentAt;

    @Column(name = "screen_share_active", nullable = false)
    private Boolean screenShareActive = false;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", length = 30, nullable = false)
    private ExamAttemptReviewStatus reviewStatus = ExamAttemptReviewStatus.NONE;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalation_reason", columnDefinition = "TEXT")
    private String escalationReason;

    @Column(name = "escalation_recommendation", length = 100)
    private String escalationRecommendation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ExamAttempt() {
    }

    public ExamAttempt(Exam exam, User student, Integer attemptNumber,
            LocalDateTime startedAt, LocalDateTime expiresAt) {
        this.exam = exam;
        this.student = student;
        this.attemptNumber = attemptNumber != null ? attemptNumber : 1;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.expiresAt = expiresAt != null ? expiresAt : this.startedAt.plusMinutes(60);
        this.status = "IN_PROGRESS";
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
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.startedAt.plusMinutes(60);
        }
        if (this.status == null) {
            this.status = "IN_PROGRESS";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime serverNow) {
        if ("SUBMITTED".equalsIgnoreCase(this.status)) {
            return false;
        }
        return this.expiresAt != null && serverNow != null && serverNow.isAfter(this.expiresAt);
    }

    public long getRemainingSeconds(LocalDateTime serverNow) {
        if (this.expiresAt == null || serverNow == null) {
            return 0;
        }
        if (serverNow.isAfter(this.expiresAt)) {
            return 0;
        }
        return Duration.between(serverNow, this.expiresAt).toSeconds();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(BigDecimal obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
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

    public String getSetLabel() {
        return setLabel;
    }

    public void setSetLabel(String setLabel) {
        this.setLabel = setLabel;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public String getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(String questionOrder) {
        this.questionOrder = questionOrder;
    }

    public String getOptionShuffleMap() {
        return optionShuffleMap;
    }

    public void setOptionShuffleMap(String optionShuffleMap) {
        this.optionShuffleMap = optionShuffleMap;
    }

    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven != null ? consentGiven : false;
    }

    public LocalDateTime getConsentGivenAt() {
        return consentGivenAt;
    }

    public void setConsentGivenAt(LocalDateTime consentGivenAt) {
        this.consentGivenAt = consentGivenAt;
    }

    public Integer getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(Integer tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount != null ? tabSwitchCount : 0;
    }

    public Integer getFullscreenExitCount() {
        return fullscreenExitCount;
    }

    public void setFullscreenExitCount(Integer fullscreenExitCount) {
        this.fullscreenExitCount = fullscreenExitCount != null ? fullscreenExitCount : 0;
    }

    public String getMonitoringFlags() {
        return monitoringFlags;
    }

    public void setMonitoringFlags(String monitoringFlags) {
        this.monitoringFlags = monitoringFlags;
    }

    public Boolean getScreenShareConsentGiven() {
        return screenShareConsentGiven;
    }

    public void setScreenShareConsentGiven(Boolean screenShareConsentGiven) {
        this.screenShareConsentGiven = screenShareConsentGiven != null ? screenShareConsentGiven : false;
    }

    public LocalDateTime getScreenShareConsentAt() {
        return screenShareConsentAt;
    }

    public void setScreenShareConsentAt(LocalDateTime screenShareConsentAt) {
        this.screenShareConsentAt = screenShareConsentAt;
    }

    public Boolean getScreenShareActive() {
        return screenShareActive;
    }

    public void setScreenShareActive(Boolean screenShareActive) {
        this.screenShareActive = screenShareActive != null ? screenShareActive : false;
    }

    public Boolean getIsPublished() {
        return isPublished != null ? isPublished : false;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished != null ? isPublished : false;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Boolean getIsLocked() {
        return isLocked != null ? isLocked : false;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked != null ? isLocked : false;
    }

    public ExamAttemptReviewStatus getReviewStatus() {
        return reviewStatus != null ? reviewStatus : ExamAttemptReviewStatus.NONE;
    }

    public void setReviewStatus(ExamAttemptReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus != null ? reviewStatus : ExamAttemptReviewStatus.NONE;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public String getEscalationRecommendation() {
        return escalationRecommendation;
    }

    public void setEscalationRecommendation(String escalationRecommendation) {
        this.escalationRecommendation = escalationRecommendation;
    }
}
