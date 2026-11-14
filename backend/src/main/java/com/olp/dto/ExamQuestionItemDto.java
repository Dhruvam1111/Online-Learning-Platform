package com.olp.dto;

import java.math.BigDecimal;

/**
 * DTO representing an exam question delivered to the client during an exam attempt (FR-I25, Step 38).
 * Note: Excludes correct answers and grading rubrics for student integrity.
 */
public class ExamQuestionItemDto {

    private Integer id;
    private Integer questionId;
    private String questionText;
    private String questionType;
    private String optionsJson;
    private BigDecimal marks;
    private Integer orderIndex;

    public ExamQuestionItemDto() {}

    public ExamQuestionItemDto(Integer id, Integer questionId, String questionText,
                               String questionType, String optionsJson,
                               BigDecimal marks, Integer orderIndex) {
        this.id = id;
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.optionsJson = optionsJson;
        this.marks = marks;
        this.orderIndex = orderIndex;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public BigDecimal getMarks() {
        return marks;
    }

    public void setMarks(BigDecimal marks) {
        this.marks = marks;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}

