package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO returning the generated exam set variants for instructor preview and verification (FR-I27, Step 39).
 */
public class ExamSetGenerationResponse {

    private Integer examId;
    private String examTitle;
    private Long masterSeed;
    private Integer totalSetsGenerated;
    private List<ExamSetVariantDto> variants = new ArrayList<>();

    public ExamSetGenerationResponse() {}

    public ExamSetGenerationResponse(Integer examId, String examTitle, Long masterSeed,
                                     Integer totalSetsGenerated, List<ExamSetVariantDto> variants) {
        this.examId = examId;
        this.examTitle = examTitle;
        this.masterSeed = masterSeed;
        this.totalSetsGenerated = totalSetsGenerated;
        this.variants = variants != null ? variants : new ArrayList<>();
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public void setExamTitle(String examTitle) {
        this.examTitle = examTitle;
    }

    public Long getMasterSeed() {
        return masterSeed;
    }

    public void setMasterSeed(Long masterSeed) {
        this.masterSeed = masterSeed;
    }

    public Integer getTotalSetsGenerated() {
        return totalSetsGenerated;
    }

    public void setTotalSetsGenerated(Integer totalSetsGenerated) {
        this.totalSetsGenerated = totalSetsGenerated;
    }

    public List<ExamSetVariantDto> getVariants() {
        return variants;
    }

    public void setVariants(List<ExamSetVariantDto> variants) {
        this.variants = variants;
    }
}
