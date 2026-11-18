package com.olp.dto;

public class CourseEnrollmentComparisonDto {

    private Integer courseId;
    private String courseTitle;
    private String courseStatus;
    private long totalEnrollments;
    private long activeStudents;
    private long droppedStudents;
    private long completedStudents;
    private double completionRate;
    private long recentEnrollments;

    public CourseEnrollmentComparisonDto() {
    }

    public CourseEnrollmentComparisonDto(
            Integer courseId,
            String courseTitle,
            String courseStatus,
            long totalEnrollments,
            long activeStudents,
            long droppedStudents,
            long completedStudents,
            double completionRate,
            long recentEnrollments) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.courseStatus = courseStatus;
        this.totalEnrollments = totalEnrollments;
        this.activeStudents = activeStudents;
        this.droppedStudents = droppedStudents;
        this.completedStudents = completedStudents;
        this.completionRate = completionRate;
        this.recentEnrollments = recentEnrollments;
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

    public String getCourseStatus() {
        return courseStatus;
    }

    public void setCourseStatus(String courseStatus) {
        this.courseStatus = courseStatus;
    }

    public long getTotalEnrollments() {
        return totalEnrollments;
    }

    public void setTotalEnrollments(long totalEnrollments) {
        this.totalEnrollments = totalEnrollments;
    }

    public long getActiveStudents() {
        return activeStudents;
    }

    public void setActiveStudents(long activeStudents) {
        this.activeStudents = activeStudents;
    }

    public long getDroppedStudents() {
        return droppedStudents;
    }

    public void setDroppedStudents(long droppedStudents) {
        this.droppedStudents = droppedStudents;
    }

    public long getCompletedStudents() {
        return completedStudents;
    }

    public void setCompletedStudents(long completedStudents) {
        this.completedStudents = completedStudents;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public long getRecentEnrollments() {
        return recentEnrollments;
    }

    public void setRecentEnrollments(long recentEnrollments) {
        this.recentEnrollments = recentEnrollments;
    }
}
