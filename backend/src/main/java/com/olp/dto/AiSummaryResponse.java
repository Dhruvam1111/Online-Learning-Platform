package com.olp.dto;

import java.util.List;

public class AiSummaryResponse {

    private Integer courseId;
    private String sourceType;
    private Integer sourceId;
    private String sourceTitle;
    private String summary;
    private List<String> keyTakeaways;
    private List<String> suggestedReviewTopics;

    public AiSummaryResponse() {}

    public AiSummaryResponse(Integer courseId, String sourceType, Integer sourceId, String sourceTitle,
                             String summary, List<String> keyTakeaways, List<String> suggestedReviewTopics) {
        this.courseId = courseId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceTitle = sourceTitle;
        this.summary = summary;
        this.keyTakeaways = keyTakeaways;
        this.suggestedReviewTopics = suggestedReviewTopics;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyTakeaways() {
        return keyTakeaways;
    }

    public void setKeyTakeaways(List<String> keyTakeaways) {
        this.keyTakeaways = keyTakeaways;
    }

    public List<String> getSuggestedReviewTopics() {
        return suggestedReviewTopics;
    }

    public void setSuggestedReviewTopics(List<String> suggestedReviewTopics) {
        this.suggestedReviewTopics = suggestedReviewTopics;
    }
}
