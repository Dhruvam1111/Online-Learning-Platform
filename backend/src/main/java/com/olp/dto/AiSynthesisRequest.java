package com.olp.dto;

public class AiSynthesisRequest {

    private Integer notesDocumentId;
    private String transcriptText;
    private String preferredModel = "GOOGLE_GEMINI";
    private Double temperature = 0.2;
    private Boolean includeQuizQuestions = true;
    private Integer questionCount = 5;
    private Boolean includeFlashcards = true;
    private String customFocusInstructions;

    public AiSynthesisRequest() {}

    public Integer getNotesDocumentId() { return notesDocumentId; }
    public void setNotesDocumentId(Integer notesDocumentId) { this.notesDocumentId = notesDocumentId; }

    public String getTranscriptText() { return transcriptText; }
    public void setTranscriptText(String transcriptText) { this.transcriptText = transcriptText; }

    public String getPreferredModel() { return preferredModel; }
    public void setPreferredModel(String preferredModel) { this.preferredModel = preferredModel; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Boolean getIncludeQuizQuestions() { return includeQuizQuestions; }
    public void setIncludeQuizQuestions(Boolean includeQuizQuestions) { this.includeQuizQuestions = includeQuizQuestions; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Boolean getIncludeFlashcards() { return includeFlashcards; }
    public void setIncludeFlashcards(Boolean includeFlashcards) { this.includeFlashcards = includeFlashcards; }

    public String getCustomFocusInstructions() { return customFocusInstructions; }
    public void setCustomFocusInstructions(String customFocusInstructions) { this.customFocusInstructions = customFocusInstructions; }
}
