package com.olp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO representing an individual blueprint rule for exam set generation (FR-I27, Step 39).
 */
public class ExamBlueprintItemDto {

    private Integer id;
    private String topic;
    private String difficulty;

    @NotNull(message = "Question count is required")
    @Min(value = 1, message = "Question count must be at least 1")
    private Integer questionCount = 1;

    @NotNull(message = "Marks per question is required")
    @DecimalMin(value = "0.5", message = "Marks per question must be at least 0.5")
    private BigDecimal marksPerQuestion = BigDecimal.ONE;

    private Integer orderIndex = 1;

    public ExamBlueprintItemDto() {}

    public ExamBlueprintItemDto(Integer id, String topic, String difficulty,
                                Integer questionCount, BigDecimal marksPerQuestion, Integer orderIndex) {
        this.id = id;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionCount = questionCount != null ? questionCount : 1;
        this.marksPerQuestion = marksPerQuestion != null ? marksPerQuestion : BigDecimal.ONE;
        this.orderIndex = orderIndex != null ? orderIndex : 1;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public BigDecimal getMarksPerQuestion() {
        return marksPerQuestion;
    }

    public void setMarksPerQuestion(BigDecimal marksPerQuestion) {
        this.marksPerQuestion = marksPerQuestion;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
