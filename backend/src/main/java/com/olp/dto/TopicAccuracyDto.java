package com.olp.dto;

public class TopicAccuracyDto {

    private String topic;
    private Integer totalAttempted;
    private Integer correctCount;
    private Double accuracyPercentage;
    private String status; // WEAK, MODERATE, STRONG

    public TopicAccuracyDto() {}

    public TopicAccuracyDto(String topic, Integer totalAttempted, Integer correctCount,
                            Double accuracyPercentage, String status) {
        this.topic = topic;
        this.totalAttempted = totalAttempted;
        this.correctCount = correctCount;
        this.accuracyPercentage = accuracyPercentage;
        this.status = status;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getTotalAttempted() {
        return totalAttempted;
    }

    public void setTotalAttempted(Integer totalAttempted) {
        this.totalAttempted = totalAttempted;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(Double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
