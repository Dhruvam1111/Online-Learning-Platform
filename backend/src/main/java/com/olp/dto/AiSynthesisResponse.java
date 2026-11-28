package com.olp.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AiSynthesisResponse {

    private Integer id;
    private Integer lessonId;
    private String lessonTitle;
    private Integer notesDocumentId;
    private Integer instructorId;
    private String modelProvider;
    private String modelVersion;
    private Integer promptTokensUsed;
    private Integer completionTokensUsed;
    private Integer executionTimeMs;

    private String executiveSummary;
    private String fullStudyGuideMarkdown;
    private List<GlossaryItemDto> glossary;
    private List<SpokenNuanceDto> spokenNuances;
    private List<AiQuizQuestionDto> quizQuestions;
    private List<FlashcardDto> flashcards;

    private String reviewStatus;
    private LocalDateTime reviewedAt;
    private Boolean publishedToStudents;
    private LocalDateTime publishedAt;
    private String instructorNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AiSynthesisResponse() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

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

    public List<GlossaryItemDto> getGlossary() { return glossary; }
    public void setGlossary(List<GlossaryItemDto> glossary) { this.glossary = glossary; }

    public List<SpokenNuanceDto> getSpokenNuances() { return spokenNuances; }
    public void setSpokenNuances(List<SpokenNuanceDto> spokenNuances) { this.spokenNuances = spokenNuances; }

    public List<AiQuizQuestionDto> getQuizQuestions() { return quizQuestions; }
    public void setQuizQuestions(List<AiQuizQuestionDto> quizQuestions) { this.quizQuestions = quizQuestions; }

    public List<FlashcardDto> getFlashcards() { return flashcards; }
    public void setFlashcards(List<FlashcardDto> flashcards) { this.flashcards = flashcards; }

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
