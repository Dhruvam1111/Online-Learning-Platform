package com.olp.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseRosterResponse {

    private Integer courseId;
    private String courseTitle;
    private CourseRosterSummaryResponse summary;
    private List<EnrolledStudentResponse> students = new ArrayList<>();
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public CourseRosterResponse() {
    }

    public CourseRosterResponse(
            Integer courseId,
            String courseTitle,
            CourseRosterSummaryResponse summary,
            List<EnrolledStudentResponse> students,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean last) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.summary = summary;
        this.students = students != null ? students : new ArrayList<>();
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    public List<EnrolledStudentResponse> getContent() {
        return students;
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

    public CourseRosterSummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(CourseRosterSummaryResponse summary) {
        this.summary = summary;
    }

    public List<EnrolledStudentResponse> getStudents() {
        return students;
    }

    public void setStudents(List<EnrolledStudentResponse> students) {
        this.students = students != null ? students : new ArrayList<>();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
