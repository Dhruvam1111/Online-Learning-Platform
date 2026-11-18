package com.olp.dto;

public class CourseRosterSummaryResponse {

    private Integer courseId;
    private String courseTitle;
    private long totalEnrolled;
    private long completedCount;
    private long inProgressCount;
    private long notStartedCount;
    private double averageProgressPercentage;
    private long activeLast30DaysCount;

    public CourseRosterSummaryResponse() {
    }

    public CourseRosterSummaryResponse(
            Integer courseId,
            String courseTitle,
            long totalEnrolled,
            long completedCount,
            long inProgressCount,
            long notStartedCount,
            double averageProgressPercentage,
            long activeLast30DaysCount) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalEnrolled = totalEnrolled;
        this.completedCount = completedCount;
        this.inProgressCount = inProgressCount;
        this.notStartedCount = notStartedCount;
        this.averageProgressPercentage = averageProgressPercentage;
        this.activeLast30DaysCount = activeLast30DaysCount;
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

    public long getTotalEnrolled() {
        return totalEnrolled;
    }

    public void setTotalEnrolled(long totalEnrolled) {
        this.totalEnrolled = totalEnrolled;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public long getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(long inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public long getNotStartedCount() {
        return notStartedCount;
    }

    public void setNotStartedCount(long notStartedCount) {
        this.notStartedCount = notStartedCount;
    }

    public double getAverageProgressPercentage() {
        return averageProgressPercentage;
    }

    public void setAverageProgressPercentage(double averageProgressPercentage) {
        this.averageProgressPercentage = averageProgressPercentage;
    }

    public long getActiveLast30DaysCount() {
        return activeLast30DaysCount;
    }

    public void setActiveLast30DaysCount(long activeLast30DaysCount) {
        this.activeLast30DaysCount = activeLast30DaysCount;
    }
}
