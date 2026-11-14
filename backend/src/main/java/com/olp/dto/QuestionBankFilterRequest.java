package com.olp.dto;

/**
 * Filter and search criteria for querying the Question Bank (FR-I24, Step 26).
 * Supports search by topic, tag, difficulty, type, origin, and keyword full-text query.
 */
public class QuestionBankFilterRequest {

    private Integer courseId;
    private String topic;
    private String tag;
    private String difficulty;
    private String questionType;
    private String origin;
    private String search;
    private Boolean bankOnly = Boolean.TRUE;
    private Boolean isLatest = Boolean.TRUE;

    public QuestionBankFilterRequest() {}

    public QuestionBankFilterRequest(Integer courseId,
                                     String topic,
                                     String tag,
                                     String difficulty,
                                     String questionType,
                                     String origin,
                                     String search) {
        this.courseId = courseId;
        this.topic = topic;
        this.tag = tag;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.origin = origin;
        this.search = search;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Boolean getBankOnly() {
        return bankOnly;
    }

    public void setBankOnly(Boolean bankOnly) {
        this.bankOnly = bankOnly;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
    }
}
