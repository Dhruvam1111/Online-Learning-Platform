package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a formal examination in a course (FR-I25, Step 38).
 * Manages start/end windows (in UTC), duration, eligibility rules (prerequisites, progress, enrollment),
 * and links to question bank items.
 */
@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "start_window", nullable = false)
    private LocalDateTime startWindow;

    @Column(name = "end_window", nullable = false)
    private LocalDateTime endWindow;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "time_zone", length = 50, nullable = false)
    private String timeZone = "UTC";

    @Column(name = "max_marks", precision = 8, scale = 2, nullable = false)
    private BigDecimal maxMarks = new BigDecimal("100.00");

    @Column(name = "passing_marks", precision = 8, scale = 2)
    private BigDecimal passingMarks;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 1;

    @Column(name = "min_progress_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal minProgressPercentage = BigDecimal.ZERO;

    @Column(name = "require_prerequisites", nullable = false)
    private Boolean requirePrerequisites = true;

    @Column(name = "require_enrollment", nullable = false)
    private Boolean requireEnrollment = true;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "SCHEDULED";

    @Column(name = "is_randomized", nullable = false)
    private Boolean isRandomized = false;

    @Column(name = "random_seed")
    private Long randomSeed;

    @Column(name = "total_sets", nullable = false)
    private Integer totalSets = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_correction_policy", length = 30, nullable = false)
    private QuestionCorrectionPolicy questionCorrectionPolicy = QuestionCorrectionPolicy.AWARD_FULL_MARKS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ExamQuestion> examQuestions = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ExamBlueprint> blueprints = new ArrayList<>();

    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private ExamMonitoringConfig monitoringConfig;

    public Exam() {}

    public Exam(Course course, User createdBy, String title, String description, String instructions,
                LocalDateTime startWindow, LocalDateTime endWindow, Integer durationMinutes,
                String timeZone, BigDecimal maxMarks, BigDecimal passingMarks, Integer maxAttempts,
                BigDecimal minProgressPercentage, Boolean requirePrerequisites, Boolean requireEnrollment,
                String status) {
        this.course = course;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.startWindow = startWindow;
        this.endWindow = endWindow;
        this.durationMinutes = durationMinutes != null ? durationMinutes : 60;
        this.timeZone = timeZone != null ? timeZone : "UTC";
        this.maxMarks = maxMarks != null ? maxMarks : new BigDecimal("100.00");
        this.passingMarks = passingMarks;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 1;
        this.minProgressPercentage = minProgressPercentage != null ? minProgressPercentage : BigDecimal.ZERO;
        this.requirePrerequisites = requirePrerequisites != null ? requirePrerequisites : true;
        this.requireEnrollment = requireEnrollment != null ? requireEnrollment : true;
        this.status = status != null ? status : "SCHEDULED";
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
        if (this.timeZone == null) {
            this.timeZone = "UTC";
        }
        if (this.maxMarks == null) {
            this.maxMarks = new BigDecimal("100.00");
        }
        if (this.maxAttempts == null) {
            this.maxAttempts = 1;
        }
        if (this.minProgressPercentage == null) {
            this.minProgressPercentage = BigDecimal.ZERO;
        }
        if (this.requirePrerequisites == null) {
            this.requirePrerequisites = true;
        }
        if (this.requireEnrollment == null) {
            this.requireEnrollment = true;
        }
        if (this.status == null) {
            this.status = "SCHEDULED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInsideWindow(LocalDateTime atUtc) {
        if (atUtc == null || this.startWindow == null || this.endWindow == null) {
            return false;
        }
        return !atUtc.isBefore(this.startWindow) && !atUtc.isAfter(this.endWindow);
    }

    public LocalDateTime computeExpiresAt(LocalDateTime startedAtUtc) {
        if (startedAtUtc == null || this.durationMinutes == null) {
            return this.endWindow;
        }
        LocalDateTime computed = startedAtUtc.plusMinutes(this.durationMinutes);
        if (this.endWindow != null && computed.isAfter(this.endWindow)) {
            return this.endWindow;
        }
        return computed;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getStartWindow() {
        return startWindow;
    }

    public void setStartWindow(LocalDateTime startWindow) {
        this.startWindow = startWindow;
    }

    public LocalDateTime getEndWindow() {
        return endWindow;
    }

    public void setEndWindow(LocalDateTime endWindow) {
        this.endWindow = endWindow;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public BigDecimal getMinProgressPercentage() {
        return minProgressPercentage;
    }

    public void setMinProgressPercentage(BigDecimal minProgressPercentage) {
        this.minProgressPercentage = minProgressPercentage;
    }

    public Boolean getRequirePrerequisites() {
        return requirePrerequisites;
    }

    public void setRequirePrerequisites(Boolean requirePrerequisites) {
        this.requirePrerequisites = requirePrerequisites;
    }

    public Boolean getRequireEnrollment() {
        return requireEnrollment;
    }

    public void setRequireEnrollment(Boolean requireEnrollment) {
        this.requireEnrollment = requireEnrollment;
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

    public List<ExamQuestion> getExamQuestions() {
        return examQuestions;
    }

    public void setExamQuestions(List<ExamQuestion> examQuestions) {
        this.examQuestions = examQuestions;
    }

    public Boolean getIsRandomized() {
        return isRandomized;
    }

    public void setIsRandomized(Boolean randomized) {
        isRandomized = randomized != null ? randomized : false;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public Integer getTotalSets() {
        return totalSets;
    }

    public void setTotalSets(Integer totalSets) {
        this.totalSets = totalSets != null ? totalSets : 1;
    }

    public List<ExamBlueprint> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(List<ExamBlueprint> blueprints) {
        this.blueprints = blueprints;
    }

    public ExamMonitoringConfig getMonitoringConfig() {
        return monitoringConfig;
    }

    public void setMonitoringConfig(ExamMonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    public QuestionCorrectionPolicy getQuestionCorrectionPolicy() {
        return questionCorrectionPolicy;
    }

    public void setQuestionCorrectionPolicy(QuestionCorrectionPolicy questionCorrectionPolicy) {
        this.questionCorrectionPolicy = questionCorrectionPolicy != null ? questionCorrectionPolicy : QuestionCorrectionPolicy.AWARD_FULL_MARKS;
    }
}

