package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseAssessmentsOverviewResponse {

    private Integer courseId;
    private String courseTitle;
    private int totalAssessments;
    private Double overallAveragePassRate;
    private List<AssessmentPerformanceSummaryDto> assessments = new ArrayList<>();

    public CourseAssessmentsOverviewResponse() {
    }

    public CourseAssessmentsOverviewResponse(
            Integer courseId,
            String courseTitle,
            int totalAssessments,
            Double overallAveragePassRate,
            List<AssessmentPerformanceSummaryDto> assessments) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalAssessments = totalAssessments;
        this.overallAveragePassRate = overallAveragePassRate;
        this.assessments = assessments != null ? assessments : new ArrayList<>();
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

    public int getTotalAssessments() {
        return totalAssessments;
    }

    public void setTotalAssessments(int totalAssessments) {
        this.totalAssessments = totalAssessments;
    }

    public Double getOverallAveragePassRate() {
        return overallAveragePassRate;
    }

    public void setOverallAveragePassRate(Double overallAveragePassRate) {
        this.overallAveragePassRate = overallAveragePassRate;
    }

    public List<AssessmentPerformanceSummaryDto> getAssessments() {
        return assessments;
    }

    public void setAssessments(List<AssessmentPerformanceSummaryDto> assessments) {
        this.assessments = assessments != null ? assessments : new ArrayList<>();
    }
}
