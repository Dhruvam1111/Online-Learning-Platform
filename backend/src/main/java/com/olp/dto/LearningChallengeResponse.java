package com.olp.dto;

import com.olp.entity.LearningChallenge;
import com.olp.entity.StudentChallengeProgress;
import java.time.LocalDateTime;

public class LearningChallengeResponse {

    private Integer id;
    private Integer courseId;
    private String courseTitle;
    private String title;
    private String description;
    private Integer pointsReward;
    private String targetType;
    private Integer targetCount;
    private Integer currentProgress;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;

    public LearningChallengeResponse() {}

    public static LearningChallengeResponse fromEntity(LearningChallenge challenge, StudentChallengeProgress progress) {
        if (challenge == null) return null;
        LearningChallengeResponse dto = new LearningChallengeResponse();
        dto.setId(challenge.getId());
        if (challenge.getCourse() != null) {
            dto.setCourseId(challenge.getCourse().getId());
            dto.setCourseTitle(challenge.getCourse().getTitle());
        }
        dto.setTitle(challenge.getTitle());
        dto.setDescription(challenge.getDescription());
        dto.setPointsReward(challenge.getPointsReward());
        dto.setTargetType(challenge.getTargetType());
        dto.setTargetCount(challenge.getTargetCount());
        dto.setStartDate(challenge.getStartDate());
        dto.setEndDate(challenge.getEndDate());
        dto.setIsActive(challenge.getIsActive());

        if (progress != null) {
            dto.setCurrentProgress(progress.getCurrentProgress());
            dto.setIsCompleted(progress.getIsCompleted());
            dto.setCompletedAt(progress.getCompletedAt());
        } else {
            dto.setCurrentProgress(0);
            dto.setIsCompleted(false);
        }
        return dto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPointsReward() {
        return pointsReward;
    }

    public void setPointsReward(Integer pointsReward) {
        this.pointsReward = pointsReward;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }

    public Integer getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        this.currentProgress = currentProgress;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
