package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a student's submission for an assignment (FR-I68, Step 28).
 * Tracks late status, applied penalty, and feeds into the grading queue (FR-I30).
 */
@Entity
@Table(name = "assignment_submissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_assignment_student", columnNames = {"assignment_id", "student_id"})
})
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "submission_type", length = 50, nullable = false)
    private String submissionType = "FILE";

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "is_late", nullable = false)
    private Boolean isLate = false;

    @Column(name = "applied_penalty_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal appliedPenaltyPercentage = BigDecimal.ZERO;

    @Column(name = "grading_status", length = 50, nullable = false)
    private String gradingStatus = "PENDING";

    @Column(name = "awarded_marks", precision = 8, scale = 2)
    private BigDecimal awardedMarks;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grader_id")
    private User grader;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AssignmentSubmission() {}

    public AssignmentSubmission(Assignment assignment, User student, String submissionType,
                                String fileUrl, String fileName, Long fileSizeBytes,
                                String textContent, LocalDateTime submittedAt,
                                Boolean isLate, BigDecimal appliedPenaltyPercentage) {
        this.assignment = assignment;
        this.student = student;
        this.submissionType = submissionType != null ? submissionType : "FILE";
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.textContent = textContent;
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        this.isLate = Boolean.TRUE.equals(isLate);
        this.appliedPenaltyPercentage = appliedPenaltyPercentage != null ? appliedPenaltyPercentage : BigDecimal.ZERO;
        this.gradingStatus = "PENDING";
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
        if (this.submittedAt == null) {
            this.submittedAt = now;
        }
        if (this.isLate == null) {
            this.isLate = false;
        }
        if (this.appliedPenaltyPercentage == null) {
            this.appliedPenaltyPercentage = BigDecimal.ZERO;
        }
        if (this.gradingStatus == null) {
            this.gradingStatus = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getIsLate() {
        return isLate;
    }

    public void setIsLate(Boolean late) {
        isLate = late;
    }

    public BigDecimal getAppliedPenaltyPercentage() {
        return appliedPenaltyPercentage;
    }

    public void setAppliedPenaltyPercentage(BigDecimal appliedPenaltyPercentage) {
        this.appliedPenaltyPercentage = appliedPenaltyPercentage;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public BigDecimal getAwardedMarks() {
        return awardedMarks;
    }

    public void setAwardedMarks(BigDecimal awardedMarks) {
        this.awardedMarks = awardedMarks;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public User getGrader() {
        return grader;
    }

    public void setGrader(User grader) {
        this.grader = grader;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
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

