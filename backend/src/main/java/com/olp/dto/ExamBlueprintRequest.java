package com.olp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for configuring exam blueprints and randomization settings (FR-I27, Step 39).
 */
public class ExamBlueprintRequest {

    private Boolean isRandomized = true;

    private Long randomSeed;

    @Min(value = 1, message = "Total sets must be at least 1")
    private Integer totalSets = 1;

    @Valid
    private List<ExamBlueprintItemDto> rules = new ArrayList<>();

    public ExamBlueprintRequest() {}

    public Boolean getIsRandomized() {
        return isRandomized;
    }

    public void setIsRandomized(Boolean randomized) {
        isRandomized = randomized;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public Integer getTotalSets() {
        return totalSets;
    }

    public void setTotalSets(Integer totalSets) {
        this.totalSets = totalSets;
    }

    public List<ExamBlueprintItemDto> getRules() {
        return rules;
    }

    public void setRules(List<ExamBlueprintItemDto> rules) {
        this.rules = rules;
    }
}
