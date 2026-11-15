package com.olp.dto;

import java.util.List;

public class QuestionExplanationResponse {

    private Integer questionId;
    private String questionText;
    private String questionType;
    private String topic;
    private String correctAnswer;
    private String explanation;
    private String keyConcept;
    private List<String> stepByStepSolution;
    private String recommendedStudyTopic;

    public QuestionExplanationResponse() {}

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getKeyConcept() {
        return keyConcept;
    }

    public void setKeyConcept(String keyConcept) {
        this.keyConcept = keyConcept;
    }

    public List<String> getStepByStepSolution() {
        return stepByStepSolution;
    }

    public void setStepByStepSolution(List<String> stepByStepSolution) {
        this.stepByStepSolution = stepByStepSolution;
    }

    public String getRecommendedStudyTopic() {
        return recommendedStudyTopic;
    }

    public void setRecommendedStudyTopic(String recommendedStudyTopic) {
        this.recommendedStudyTopic = recommendedStudyTopic;
    }
}
