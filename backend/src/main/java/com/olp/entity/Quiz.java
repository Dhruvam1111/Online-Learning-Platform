package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a quiz linked to a Course or Module, with optional Lesson position (FR-I19).
 */
@Entity
@Table(name = "quizzes")
public class Quiz {

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
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status = QuizStatus.DRAFT;

    @Column(name = "total_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalMarks = BigDecimal.ZERO;

    @Column(name = "passing_marks", precision = 8, scale = 2)
    private BigDecimal passingMarks;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "number_of_questions")
    private Integer numberOfQuestions;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 1;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_until")
    private LocalDateTime availableUntil;

    @Column(name = "feedback_released", nullable = false)
    private Boolean feedbackReleased = true;

    @Column(name = "negative_marking", nullable = false)
    private Boolean negativeMarking = false;

    @Column(name = "partial_marking", nullable = false)
    private Boolean partialMarking = false;

    @Column(name = "numeric_tolerance", precision = 8, scale = 2)
    private BigDecimal numericTolerance = BigDecimal.ZERO;

    @Column(name = "show_correct_answers", nullable = false)
    private Boolean showCorrectAnswers = true;

    @Column(name = "show_feedback", nullable = false)
    private Boolean showFeedback = true;

    @Column(name = "answer_release_timing", nullable = false, length = 30)
    private String answerReleaseTiming = "IMMEDIATELY";

    @Column(name = "is_exam", nullable = false)
    private Boolean isExam = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    public Quiz() {}

    public Quiz(Course course, CourseModule module, Lesson lesson, User createdBy,
                String title, String description, String instructions, QuizStatus status) {
        this.course = course;
        this.module = module;
        this.lesson = lesson;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.instructions = instructions;
        this.status = status != null ? status : QuizStatus.DRAFT;
        this.totalMarks = BigDecimal.ZERO;
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
        if (this.status == null) {
            this.status = QuizStatus.DRAFT;
        }
        if (this.totalMarks == null) {
            this.totalMarks = BigDecimal.ZERO;
        }
        if (this.maxAttempts == null) {
            this.maxAttempts = 1;
        }
        if (this.negativeMarking == null) {
            this.negativeMarking = false;
        }
        if (this.partialMarking == null) {
            this.partialMarking = false;
        }
        if (this.numericTolerance == null) {
            this.numericTolerance = BigDecimal.ZERO;
        }
        if (this.isExam == null) {
            this.isExam = false;
        }
        if (Boolean.TRUE.equals(this.isExam)) {
            if (this.showCorrectAnswers == null) {
                this.showCorrectAnswers = false;
            }
            if (this.showFeedback == null) {
                this.showFeedback = false;
            }
            if (this.answerReleaseTiming == null) {
                this.answerReleaseTiming = "AFTER_DEADLINE";
            }
        } else {
            if (this.showCorrectAnswers == null) {
                this.showCorrectAnswers = true;
            }
            if (this.showFeedback == null) {
                this.showFeedback = true;
            }
            if (this.answerReleaseTiming == null) {
                this.answerReleaseTiming = "IMMEDIATELY";
            }
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

    public QuizStatus getStatus() {
        return status;
    }

    public void setStatus(QuizStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public BigDecimal getPassingMarks() {
        return passingMarks;
    }

    public void setPassingMarks(BigDecimal passingMarks) {
        this.passingMarks = passingMarks;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalDateTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalDateTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public Boolean getFeedbackReleased() {
        return feedbackReleased != null ? feedbackReleased : true;
    }

    public void setFeedbackReleased(Boolean feedbackReleased) {
        this.feedbackReleased = feedbackReleased != null ? feedbackReleased : true;
    }

    public Boolean getNegativeMarking() {
        return negativeMarking != null ? negativeMarking : false;
    }

    public void setNegativeMarking(Boolean negativeMarking) {
        this.negativeMarking = negativeMarking != null ? negativeMarking : false;
    }

    public Boolean isNegativeMarking() {
        return getNegativeMarking();
    }

    public Boolean getPartialMarking() {
        return partialMarking != null ? partialMarking : false;
    }

    public void setPartialMarking(Boolean partialMarking) {
        this.partialMarking = partialMarking != null ? partialMarking : false;
    }

    public Boolean isPartialMarking() {
        return getPartialMarking();
    }

    public BigDecimal getNumericTolerance() {
        return numericTolerance != null ? numericTolerance : BigDecimal.ZERO;
    }

    public void setNumericTolerance(BigDecimal numericTolerance) {
        this.numericTolerance = numericTolerance != null ? numericTolerance : BigDecimal.ZERO;
    }

    public com.olp.dto.GradingPolicy toGradingPolicy() {
        return new com.olp.dto.GradingPolicy(
                isNegativeMarking(),
                isPartialMarking(),
                getNumericTolerance()
        );
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

    public Boolean getShowCorrectAnswers() {
        return showCorrectAnswers;
    }

    public void setShowCorrectAnswers(Boolean showCorrectAnswers) {
        this.showCorrectAnswers = showCorrectAnswers;
    }

    public Boolean isShowCorrectAnswers() {
        return Boolean.TRUE.equals(this.showCorrectAnswers);
    }

    public Boolean getShowFeedback() {
        return showFeedback;
    }

    public void setShowFeedback(Boolean showFeedback) {
        this.showFeedback = showFeedback;
    }

    public Boolean isShowFeedback() {
        return Boolean.TRUE.equals(this.showFeedback);
    }

    public String getAnswerReleaseTiming() {
        return answerReleaseTiming != null ? answerReleaseTiming : "IMMEDIATELY";
    }

    public void setAnswerReleaseTiming(String answerReleaseTiming) {
        this.answerReleaseTiming = answerReleaseTiming;
    }

    public Boolean getIsExam() {
        return isExam;
    }

    public void setIsExam(Boolean exam) {
        this.isExam = exam;
    }

    public boolean isExam() {
        return Boolean.TRUE.equals(this.isExam);
    }

    /**
     * Determines whether students are currently permitted to review correct answers (FR-I23, Step 27).
     * Server-side authority: checks toggle flag, release timing rule, and deadline timestamp in UTC.
     */
    public boolean isCorrectAnswersVisible(LocalDateTime atUtc) {
        if (!Boolean.TRUE.equals(this.showCorrectAnswers)) {
            return false;
        }
        String timing = getAnswerReleaseTiming();
        if ("NEVER".equalsIgnoreCase(timing)) {
            return false;
        }
        if ("AFTER_DEADLINE".equalsIgnoreCase(timing)) {
            if (this.availableUntil == null) {
                return false;
            }
            return atUtc != null && !atUtc.isBefore(this.availableUntil);
        }
        return true; // IMMEDIATELY
    }

    /**
     * Determines whether students are currently permitted to review feedback and explanations (FR-I23, Step 27).
     * Server-side authority: checks feedback toggle flags, release timing rule, and deadline timestamp in UTC.
     */
    public boolean isFeedbackVisible(LocalDateTime atUtc) {
        if (!Boolean.TRUE.equals(this.feedbackReleased) || !Boolean.TRUE.equals(this.showFeedback)) {
            return false;
        }
        String timing = getAnswerReleaseTiming();
        if ("NEVER".equalsIgnoreCase(timing)) {
            return false;
        }
        if ("AFTER_DEADLINE".equalsIgnoreCase(timing)) {
            if (this.availableUntil == null) {
                return false;
            }
            return atUtc != null && !atUtc.isBefore(this.availableUntil);
        }
        return true; // IMMEDIATELY
    }

    public List<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }

    public void setQuizQuestions(List<QuizQuestion> quizQuestions) {
        this.quizQuestions = quizQuestions;
    }
}

