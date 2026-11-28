package com.olp.dto;

import java.util.List;

public class AiSynthesisReviewRequest {

    private String executiveSummary;
    private String fullStudyGuideMarkdown;
    private List<GlossaryItemDto> glossary;
    private List<SpokenNuanceDto> spokenNuances;
    private List<AiQuizQuestionDto> quizQuestions;
    private List<FlashcardDto> flashcards;
    private String instructorNotes;

    public AiSynthesisReviewRequest() {}

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

    public String getInstructorNotes() { return instructorNotes; }
    public void setInstructorNotes(String instructorNotes) { this.instructorNotes = instructorNotes; }
}
