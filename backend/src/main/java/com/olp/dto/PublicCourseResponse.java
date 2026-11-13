package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PublicCourseResponse {

    private Integer id;
    private String title;
    private String description;
    private String status;
    private Integer instructorId;
    private String instructorName;
    private LocalDateTime createdAt;
    private int totalLectures;
    private Double averageRating;
    private int totalReviews;
    private boolean enrolled;
    private boolean bookmarked;
    private List<CourseInstructorResponse> instructors = new ArrayList<>();
    private BigDecimal price = BigDecimal.ZERO;
    private BigDecimal finalPayableAmount = BigDecimal.ZERO;
    private boolean isFree = true;
    private Integer enrollmentLimit;

    public PublicCourseResponse() {}

    public PublicCourseResponse(Integer id, String title, String description, String status,
                                Integer instructorId, String instructorName, LocalDateTime createdAt,
                                int totalLectures, Double averageRating, int totalReviews,
                                boolean enrolled, boolean bookmarked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.createdAt = createdAt;
        this.totalLectures = totalLectures;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.enrolled = enrolled;
        this.bookmarked = bookmarked;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalLectures() {
        return totalLectures;
    }

    public void setTotalLectures(int totalLectures) {
        this.totalLectures = totalLectures;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(boolean enrolled) {
        this.enrolled = enrolled;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public List<CourseInstructorResponse> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<CourseInstructorResponse> instructors) {
        this.instructors = instructors != null ? instructors : new ArrayList<>();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price != null ? price : BigDecimal.ZERO;
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public void setFinalPayableAmount(BigDecimal finalPayableAmount) {
        this.finalPayableAmount = finalPayableAmount != null ? finalPayableAmount : BigDecimal.ZERO;
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public Integer getEnrollmentLimit() {
        return enrollmentLimit;
    }

    public void setEnrollmentLimit(Integer enrollmentLimit) {
        this.enrollmentLimit = enrollmentLimit;
    }
}
