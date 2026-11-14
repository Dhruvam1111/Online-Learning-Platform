package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entity representing a student attempt on a scheduled examination (FR-I25, Step 38).
 * Enforces server-authoritative timers (DR-ED06) to prevent client clock tampering.
 */
@Entity
@Table(name = "exam_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_student_attempt", columnNames = {"exam_id", "student_id", "attempt_number"})
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ExamAttempt() {}

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
}

