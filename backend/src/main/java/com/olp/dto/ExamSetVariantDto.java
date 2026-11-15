package com.olp.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing an individual generated variant/set of an examination (FR-I27, Step 39).
 */
public class ExamSetVariantDto {

    private String setLabel;
    private Long seed;
    private BigDecimal totalMarks;
    private Integer questionCount;
    private List<ExamQuestionItemDto> questions = new ArrayList<>();

    public ExamSetVariantDto() {}

    public ExamSetVariantDto(String setLabel, Long seed, BigDecimal totalMarks,
                             Integer questionCount, List<ExamQuestionItemDto> questions) {
        this.setLabel = setLabel;
        this.seed = seed;
        this.totalMarks = totalMarks;
        this.questionCount = questionCount;
        this.questions = questions != null ? questions : new ArrayList<>();
    }

    public String getSetLabel() {
        return setLabel;
    }

    public void setSetLabel(String setLabel) {
        this.setLabel = setLabel;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<ExamQuestionItemDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ExamQuestionItemDto> questions) {
        this.questions = questions;
    }
}
