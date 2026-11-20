package com.olp.dto;

import java.util.List;

public class WeakTopicsResponse {

    private Integer courseId;
    private Double overallAccuracy;
    private Integer totalQuestionsEvaluated;
    private List<TopicAccuracyDto> topicBreakdown;
    private List<String> weakTopics;
    private String feedbackSummary;

    public WeakTopicsResponse() {}

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(Double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public Integer getTotalQuestionsEvaluated() {
        return totalQuestionsEvaluated;
    }

    public void setTotalQuestionsEvaluated(Integer totalQuestionsEvaluated) {
        this.totalQuestionsEvaluated = totalQuestionsEvaluated;
    }

    public List<TopicAccuracyDto> getTopicBreakdown() {
        return topicBreakdown;
    }

    public void setTopicBreakdown(List<TopicAccuracyDto> topicBreakdown) {
        this.topicBreakdown = topicBreakdown;
    }

    public List<String> getWeakTopics() {
        return weakTopics;
    }

    public void setWeakTopics(List<String> weakTopics) {
        this.weakTopics = weakTopics;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }
}
