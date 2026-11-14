package com.olp.dto;

import java.math.BigDecimal;

public class StudentQuizQuestionDto {
    private Integer questionId;
    private String questionText;
    private String questionType;
    private String optionsJson;
    private BigDecimal marks;
    private BigDecimal negativeMarks;
    private BigDecimal numericalTolerance;
    private Boolean partialMarking;
    private Integer orderIndex;
    private String savedAnswer;

    public StudentQuizQuestionDto() {}

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public BigDecimal getMarks() { return marks; }
    public void setMarks(BigDecimal marks) { this.marks = marks; }

    public BigDecimal getNegativeMarks() { return negativeMarks; }
    public void setNegativeMarks(BigDecimal negativeMarks) { this.negativeMarks = negativeMarks; }

    public BigDecimal getNumericalTolerance() { return numericalTolerance; }
    public void setNumericalTolerance(BigDecimal numericalTolerance) { this.numericalTolerance = numericalTolerance; }

    public Boolean getPartialMarking() { return partialMarking; }
    public void setPartialMarking(Boolean partialMarking) { this.partialMarking = partialMarking; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getSavedAnswer() { return savedAnswer; }
    public void setSavedAnswer(String savedAnswer) { this.savedAnswer = savedAnswer; }
}