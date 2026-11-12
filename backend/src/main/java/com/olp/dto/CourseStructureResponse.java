package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseStructureResponse {

    private Integer courseId;
    private String courseTitle;
    private String courseStatus;
    private int totalModules;
    private int totalSections;
    private int totalLessons;
    private List<CourseModuleResponse> modules = new ArrayList<>();

    public CourseStructureResponse() {}

    public CourseStructureResponse(Integer courseId, String courseTitle, String courseStatus,
                                   List<CourseModuleResponse> modules) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.courseStatus = courseStatus;
        this.modules = modules != null ? modules : new ArrayList<>();
        this.totalModules = this.modules.size();
        this.totalSections = this.modules.stream()
                .mapToInt(m -> m.getSections() != null ? m.getSections().size() : 0)
                .sum();
        this.totalLessons = this.modules.stream()
                .flatMap(m -> m.getSections() != null ? m.getSections().stream() : java.util.stream.Stream.empty())
                .mapToInt(s -> s.getLessons() != null ? s.getLessons().size() : 0)
                .sum();
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

    public int getTotalModules() {
        return totalModules;
    }

    public void setTotalModules(int totalModules) {
        this.totalModules = totalModules;
    }

    public int getTotalSections() {
        return totalSections;
    }

    public void setTotalSections(int totalSections) {
        this.totalSections = totalSections;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public List<CourseModuleResponse> getModules() {
        return modules;
    }

    public void setModules(List<CourseModuleResponse> modules) {
        this.modules = modules != null ? modules : new ArrayList<>();
    }
}

