package com.olp.dto;

import java.util.List;

public class CourseProgressResponse {

    private Integer courseId;
    private int completedCount;
    private int totalCount;
    private double percentage;
    private List<Integer> completedLectureIds;

    public CourseProgressResponse() {}

    public CourseProgressResponse(Integer courseId, int completedCount, int totalCount, double percentage, List<Integer> completedLectureIds) {
        this.courseId = courseId;
        this.completedCount = completedCount;
        this.totalCount = totalCount;
        this.percentage = percentage;
        this.completedLectureIds = completedLectureIds;
    }

    // Getters and Setters
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public List<Integer> getCompletedLectureIds() {
        return completedLectureIds;
    }

    public void setCompletedLectureIds(List<Integer> completedLectureIds) {
        this.completedLectureIds = completedLectureIds;
    }
}