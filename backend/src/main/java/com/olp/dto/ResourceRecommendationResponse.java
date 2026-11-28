package com.olp.dto;

import java.util.List;

public class ResourceRecommendationResponse {

    private Integer courseId;
    private List<String> targetedWeakTopics;
    private List<RecommendedItemDto> recommendedLectures;
    private List<RecommendedItemDto> recommendedMaterials;
    private List<RecommendedItemDto> recommendedPracticeQuestions;
    private String rationale;

    public ResourceRecommendationResponse() {}

    public static class RecommendedItemDto {
        private Integer id;
        private String title;
        private String type;
        private String topic;
        private String difficulty;
        private String url;

        public RecommendedItemDto() {}

        public RecommendedItemDto(Integer id, String title, String type, String topic, String difficulty, String url) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.topic = topic;
            this.difficulty = difficulty;
            this.url = url;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public List<String> getTargetedWeakTopics() {
        return targetedWeakTopics;
    }

    public void setTargetedWeakTopics(List<String> targetedWeakTopics) {
        this.targetedWeakTopics = targetedWeakTopics;
    }

    public List<RecommendedItemDto> getRecommendedLectures() {
        return recommendedLectures;
    }

    public void setRecommendedLectures(List<RecommendedItemDto> recommendedLectures) {
        this.recommendedLectures = recommendedLectures;
    }

    public List<RecommendedItemDto> getRecommendedMaterials() {
        return recommendedMaterials;
    }

    public void setRecommendedMaterials(List<RecommendedItemDto> recommendedMaterials) {
        this.recommendedMaterials = recommendedMaterials;
    }

    public List<RecommendedItemDto> getRecommendedPracticeQuestions() {
        return recommendedPracticeQuestions;
    }

    public void setRecommendedPracticeQuestions(List<RecommendedItemDto> recommendedPracticeQuestions) {
        this.recommendedPracticeQuestions = recommendedPracticeQuestions;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
