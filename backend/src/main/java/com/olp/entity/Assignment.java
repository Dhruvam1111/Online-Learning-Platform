package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an assignment task in a course (FR-I68, Step 28).
 * Manages deadline enforcement, late submission policies, and max marks.
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "max_marks", precision = 8, scale = 2, nullable = false)
    private BigDecimal maxMarks = new BigDecimal("100.00");

    @Column(columnDefinition = "TEXT")
    private String rubric;

    @Column(name = "submission_type", length = 50, nullable = false)
    private String submissionType = "FILE";

    @Column(name = "late_policy", length = 50, nullable = false)
    private String latePolicy = "REJECT";

    @Column(name = "late_penalty_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal latePenaltyPercentage = BigDecimal.ZERO;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Assignment() {}

    public Assignment(Course course, CourseModule module, Lesson lesson, User createdBy,
                      String title, String instructions, LocalDateTime deadline,
                      BigDecimal maxMarks, String rubric, String submissionType,
                      String latePolicy, BigDecimal latePenaltyPercentage, String status) {
        this.course = course;
        this.module = module;
        this.lesson = lesson;
        this.createdBy = createdBy;
        this.title = title;
        this.instructions = instructions;
        this.deadline = deadline;
        this.maxMarks = maxMarks != null ? maxMarks : new BigDecimal("100.00");
        this.rubric = rubric;
        this.submissionType = submissionType != null ? submissionType : "FILE";
        this.latePolicy = latePolicy != null ? latePolicy : "REJECT";
        this.latePenaltyPercentage = latePenaltyPercentage != null ? latePenaltyPercentage : BigDecimal.ZERO;
        this.status = status != null ? status : "PUBLISHED";
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
        if (this.maxMarks == null) {
            this.maxMarks = new BigDecimal("100.00");
        }
        if (this.submissionType == null) {
            this.submissionType = "FILE";
        }
        if (this.latePolicy == null) {
            this.latePolicy = "REJECT";
        }
        if (this.latePenaltyPercentage == null) {
            this.latePenaltyPercentage = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = "PUBLISHED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSubmissionLate(LocalDateTime submissionTimeUtc) {
        return this.deadline != null && submissionTimeUtc != null && submissionTimeUtc.isAfter(this.deadline);
    }

    public BigDecimal calculateLatePenalty(LocalDateTime submissionTimeUtc) {
        if (!isSubmissionLate(submissionTimeUtc)) {
            return BigDecimal.ZERO;
        }
        if ("PENALTY".equalsIgnoreCase(this.latePolicy)) {
            return this.latePenaltyPercentage != null ? this.latePenaltyPercentage : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CourseModule getModule() {
        return module;
    }

    public void setModule(CourseModule module) {
        this.module = module;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getRubric() {
        return rubric;
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
    }

    public String getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(String submissionType) {
        this.submissionType = submissionType;
    }

    public String getLatePolicy() {
        return latePolicy;
    }

    public void setLatePolicy(String latePolicy) {
        this.latePolicy = latePolicy;
    }

    public BigDecimal getLatePenaltyPercentage() {
        return latePenaltyPercentage;
    }

    public void setLatePenaltyPercentage(BigDecimal latePenaltyPercentage) {
        this.latePenaltyPercentage = latePenaltyPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

