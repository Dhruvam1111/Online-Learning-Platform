package com.olp.dto;

public class AiSynthesisPublishRequest {

    private Boolean publishStudyGuide = true;
    private Boolean exportQuestionsToQuestionBank = true;
    private String targetTopicTag;
    private Boolean publishFlashcards = true;

    public AiSynthesisPublishRequest() {}

    public Boolean getPublishStudyGuide() { return publishStudyGuide; }
    public void setPublishStudyGuide(Boolean publishStudyGuide) { this.publishStudyGuide = publishStudyGuide; }

    public Boolean getExportQuestionsToQuestionBank() { return exportQuestionsToQuestionBank; }
    public void setExportQuestionsToQuestionBank(Boolean exportQuestionsToQuestionBank) { this.exportQuestionsToQuestionBank = exportQuestionsToQuestionBank; }

    public String getTargetTopicTag() { return targetTopicTag; }
    public void setTargetTopicTag(String targetTopicTag) { this.targetTopicTag = targetTopicTag; }

    public Boolean getPublishFlashcards() { return publishFlashcards; }
    public void setPublishFlashcards(Boolean publishFlashcards) { this.publishFlashcards = publishFlashcards; }
}
