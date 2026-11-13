package com.olp.dto;

import com.olp.entity.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourseDetailResponse {

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
    private List<LectureResponse> lectures = new ArrayList<>();
    private List<CourseInstructorResponse> instructors = new ArrayList<>();
    private BigDecimal price = BigDecimal.ZERO;
    private DiscountType discountType;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal discountedPrice = BigDecimal.ZERO;
    private BigDecimal taxRate = new BigDecimal("0.18");
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal finalPayableAmount = BigDecimal.ZERO;
    private boolean isFree = true;
    private Integer enrollmentLimit;
    private boolean isEnrollmentLimitReached = false;

    public CourseDetailResponse() {}

    public CourseDetailResponse(Integer id, String title, String description, String status,
                                Integer instructorId, String instructorName, LocalDateTime createdAt,
                                int totalLectures, Double averageRating, int totalReviews,
                                boolean enrolled, boolean bookmarked, List<LectureResponse> lectures) {
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
        this.lectures = lectures != null ? lectures : new ArrayList<>();
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

    public List<LectureResponse> getLectures() {
        return lectures;
    }

    public void setLectures(List<LectureResponse> lectures) {
        this.lectures = lectures;
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

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
    }

    public LocalDateTime getDiscountStartDate() {
        return discountStartDate;
    }

    public void setDiscountStartDate(LocalDateTime discountStartDate) {
        this.discountStartDate = discountStartDate;
    }

    public LocalDateTime getDiscountEndDate() {
        return discountEndDate;
    }

    public void setDiscountEndDate(LocalDateTime discountEndDate) {
        this.discountEndDate = discountEndDate;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public void setFinalPayableAmount(BigDecimal finalPayableAmount) {
        this.finalPayableAmount = finalPayableAmount;
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

    public boolean isEnrollmentLimitReached() {
        return isEnrollmentLimitReached;
    }

    public void setEnrollmentLimitReached(boolean enrollmentLimitReached) {
        isEnrollmentLimitReached = enrollmentLimitReached;
    }
}
