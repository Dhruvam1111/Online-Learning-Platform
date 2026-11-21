package com.olp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_lecture_syntheses")
public class AiLectureSynthesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(name = "notes_document_id")
    private Integer notesDocumentId;

    @Column(name = "instructor_id", nullable = false)
    private Integer instructorId;

    @Column(name = "model_provider", nullable = false, length = 50)
    private String modelProvider;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "prompt_tokens_used")
    private Integer promptTokensUsed;

    @Column(name = "completion_tokens_used")
    private Integer completionTokensUsed;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @Column(name = "executive_summary", nullable = false, columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "full_study_guide_markdown", nullable = false, columnDefinition = "TEXT")
    private String fullStudyGuideMarkdown;

    @Column(name = "glossary_json", columnDefinition = "TEXT")
    private String glossaryJson;

    @Column(name = "spoken_nuances_json", columnDefinition = "TEXT")
    private String spokenNuancesJson;

    @Column(name = "quiz_questions_json", columnDefinition = "TEXT")
    private String quizQuestionsJson;

    @Column(name = "flashcards_json", columnDefinition = "TEXT")
    private String flashcardsJson;

    @Column(name = "review_status", nullable = false, length = 30)
    private String reviewStatus = "DRAFT";

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "published_to_students", nullable = false)
    private Boolean publishedToStudents = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "instructor_notes", columnDefinition = "TEXT")
    private String instructorNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AiLectureSynthesis() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (reviewStatus == null) reviewStatus = "DRAFT";
        if (publishedToStudents == null) publishedToStudents = false;
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public Integer getNotesDocumentId() { return notesDocumentId; }
    public void setNotesDocumentId(Integer notesDocumentId) { this.notesDocumentId = notesDocumentId; }

    public Integer getInstructorId() { return instructorId; }
    public void setInstructorId(Integer instructorId) { this.instructorId = instructorId; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Integer getPromptTokensUsed() { return promptTokensUsed; }
    public void setPromptTokensUsed(Integer promptTokensUsed) { this.promptTokensUsed = promptTokensUsed; }

    public Integer getCompletionTokensUsed() { return completionTokensUsed; }
    public void setCompletionTokensUsed(Integer completionTokensUsed) { this.completionTokensUsed = completionTokensUsed; }

    public Integer getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Integer executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }

    public String getFullStudyGuideMarkdown() { return fullStudyGuideMarkdown; }
    public void setFullStudyGuideMarkdown(String fullStudyGuideMarkdown) { this.fullStudyGuideMarkdown = fullStudyGuideMarkdown; }

    public String getGlossaryJson() { return glossaryJson; }
    public void setGlossaryJson(String glossaryJson) { this.glossaryJson = glossaryJson; }

    public String getSpokenNuancesJson() { return spokenNuancesJson; }
    public void setSpokenNuancesJson(String spokenNuancesJson) { this.spokenNuancesJson = spokenNuancesJson; }

    public String getQuizQuestionsJson() { return quizQuestionsJson; }
    public void setQuizQuestionsJson(String quizQuestionsJson) { this.quizQuestionsJson = quizQuestionsJson; }

    public String getFlashcardsJson() { return flashcardsJson; }
    public void setFlashcardsJson(String flashcardsJson) { this.flashcardsJson = flashcardsJson; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Boolean getPublishedToStudents() { return publishedToStudents; }
    public void setPublishedToStudents(Boolean publishedToStudents) { this.publishedToStudents = publishedToStudents; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public String getInstructorNotes() { return instructorNotes; }
    public void setInstructorNotes(String instructorNotes) { this.instructorNotes = instructorNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
