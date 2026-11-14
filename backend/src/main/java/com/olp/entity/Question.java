package com.olp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Reusable Question entity designed to support FR-I19, FR-I21 (question types),
 * FR-I22 (marking schemes), and FR-I24 (question bank).
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(name = "default_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal defaultMarks = BigDecimal.ONE;

    @Column(name = "negative_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal negativeMarks = BigDecimal.ZERO;

    @Column(name = "numerical_tolerance", precision = 8, scale = 2)
    private BigDecimal numericalTolerance = BigDecimal.ZERO;

    @Column(name = "grading_rubric", columnDefinition = "TEXT")
    private String gradingRubric;

    @Column(name = "requires_manual_grading", nullable = false)
    private Boolean requiresManualGrading = Boolean.FALSE;

    @Column(length = 100)
    private String topic;

    @Column(length = 30)
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "is_bank_question", nullable = false)
    private Boolean isBankQuestion = Boolean.FALSE;

    @Column(name = "origin", nullable = false, length = 30)
    private String origin = "MANUAL";

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private Question parentQuestion;

    @Column(name = "is_latest", nullable = false)
    private Boolean isLatest = Boolean.TRUE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Question() {}

    public Question(Course course, User createdBy, String questionText, QuestionType questionType,
                    String optionsJson, String correctAnswer, BigDecimal defaultMarks, BigDecimal negativeMarks) {
        this(course, createdBy, questionText, questionType, optionsJson, correctAnswer, defaultMarks, negativeMarks, BigDecimal.ZERO, null, null);
    }

    public Question(Course course, User createdBy, String questionText, QuestionType questionType,
                    String optionsJson, String correctAnswer, BigDecimal defaultMarks, BigDecimal negativeMarks,
                    BigDecimal numericalTolerance, String gradingRubric, Boolean requiresManualGrading) {
        this.course = course;
        this.createdBy = createdBy;
        this.questionText = questionText;
        this.questionType = questionType != null ? questionType : QuestionType.MULTIPLE_CHOICE;
        this.optionsJson = optionsJson;
        this.correctAnswer = correctAnswer;
        this.defaultMarks = defaultMarks != null ? defaultMarks : BigDecimal.ONE;
        this.negativeMarks = negativeMarks != null ? negativeMarks : BigDecimal.ZERO;
        this.numericalTolerance = numericalTolerance != null ? numericalTolerance : BigDecimal.ZERO;
        this.gradingRubric = gradingRubric;
        this.requiresManualGrading = requiresManualGrading != null
                ? requiresManualGrading
                : (this.questionType == QuestionType.DESCRIPTIVE);
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
        if (this.questionType == null) {
            this.questionType = QuestionType.MULTIPLE_CHOICE;
        }
        if (this.defaultMarks == null) {
            this.defaultMarks = BigDecimal.ONE;
        }
        if (this.negativeMarks == null) {
            this.negativeMarks = BigDecimal.ZERO;
        }
        if (this.numericalTolerance == null) {
            this.numericalTolerance = BigDecimal.ZERO;
        }
        if (this.isBankQuestion == null) {
            this.isBankQuestion = Boolean.FALSE;
        }
        if (this.origin == null || this.origin.trim().isEmpty()) {
            this.origin = "MANUAL";
        }
        if (this.version == null) {
            this.version = 1;
        }
        if (this.isLatest == null) {
            this.isLatest = Boolean.TRUE;
        }
        if (this.requiresManualGrading == null) {
            this.requiresManualGrading = (this.questionType == QuestionType.DESCRIPTIVE);
        } else if (this.questionType == QuestionType.DESCRIPTIVE) {
            this.requiresManualGrading = Boolean.TRUE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.questionType == QuestionType.DESCRIPTIVE) {
            this.requiresManualGrading = Boolean.TRUE;
        }
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public BigDecimal getDefaultMarks() {
        return defaultMarks;
    }

    public void setDefaultMarks(BigDecimal defaultMarks) {
        this.defaultMarks = defaultMarks;
    }

    public BigDecimal getNegativeMarks() {
        return negativeMarks;
    }

    public void setNegativeMarks(BigDecimal negativeMarks) {
        this.negativeMarks = negativeMarks;
    }

    public BigDecimal getNumericalTolerance() {
        return numericalTolerance;
    }

    public void setNumericalTolerance(BigDecimal numericalTolerance) {
        this.numericalTolerance = numericalTolerance;
    }

    public String getGradingRubric() {
        return gradingRubric;
    }

    public void setGradingRubric(String gradingRubric) {
        this.gradingRubric = gradingRubric;
    }

    public Boolean getRequiresManualGrading() {
        return requiresManualGrading;
    }

    public void setRequiresManualGrading(Boolean requiresManualGrading) {
        this.requiresManualGrading = requiresManualGrading;
    }

    public BigDecimal getMaxMarks() {
        return defaultMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.defaultMarks = maxMarks;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Boolean getIsBankQuestion() {
        return isBankQuestion;
    }

    public void setIsBankQuestion(Boolean isBankQuestion) {
        this.isBankQuestion = isBankQuestion;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin != null && !origin.trim().isEmpty() ? origin.trim().toUpperCase() : "MANUAL";
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Question getParentQuestion() {
        return parentQuestion;
    }

    public void setParentQuestion(Question parentQuestion) {
        this.parentQuestion = parentQuestion;
    }

    public Integer getParentQuestionId() {
        return parentQuestion != null ? parentQuestion.getId() : null;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
    }

    public boolean isLatestVersion() {
        return Boolean.TRUE.equals(isLatest);
    }

    public Question cloneForReuse(Course targetCourse, User actor) {
        Question copy = new Question(
                targetCourse != null ? targetCourse : this.course,
                actor != null ? actor : this.createdBy,
                this.questionText,
                this.questionType,
                this.optionsJson,
                this.correctAnswer,
                this.defaultMarks,
                this.negativeMarks,
                this.numericalTolerance,
                this.gradingRubric,
                this.requiresManualGrading
        );
        copy.setTopic(this.topic);
        copy.setDifficulty(this.difficulty);
        copy.setExplanation(this.explanation);
        copy.setIsBankQuestion(this.isBankQuestion);
        copy.setOrigin(this.origin);
        copy.setTags(this.tags);
        copy.setVersion(1);
        copy.setParentQuestion(null);
        copy.setIsLatest(Boolean.TRUE);
        return copy;
    }

    public Question createNextVersion(User actor) {
        Question root = this.parentQuestion != null ? this.parentQuestion : this;
        Question next = new Question(
                this.course,
                actor != null ? actor : this.createdBy,
                this.questionText,
                this.questionType,
                this.optionsJson,
                this.correctAnswer,
                this.defaultMarks,
                this.negativeMarks,
                this.numericalTolerance,
                this.gradingRubric,
                this.requiresManualGrading
        );
        next.setTopic(this.topic);
        next.setDifficulty(this.difficulty);
        next.setExplanation(this.explanation);
        next.setIsBankQuestion(this.isBankQuestion);
        next.setOrigin(this.origin);
        next.setTags(this.tags);
        next.setVersion((this.version != null ? this.version : 1) + 1);
        next.setParentQuestion(root);
        next.setIsLatest(Boolean.TRUE);
        return next;
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

