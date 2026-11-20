package com.olp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class StudyScheduleRequest {

    private Integer courseId;

    @NotBlank(message = "Title is required")
    private String title;

    private String taskDescription;

    private String itemType = "REVISION";

    private Integer referenceId;

    @NotNull(message = "Scheduled start time is required")
    private LocalDateTime scheduledStart;

    @NotNull(message = "Scheduled end time is required")
    private LocalDateTime scheduledEnd;

    private String priority = "MEDIUM";

    private String timeZone = "UTC";

    public StudyScheduleRequest() {}

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(LocalDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public LocalDateTime getScheduledEnd() {
        return scheduledEnd;
    }

    public void setScheduledEnd(LocalDateTime scheduledEnd) {
        this.scheduledEnd = scheduledEnd;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
