package com.olp.dto;

import com.olp.entity.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateCourseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    private String description;

    @Size(max = 100, message = "Category must be 100 characters or fewer")
    private String category = "General";

    private List<String> learningOutcomes = new ArrayList<>();

    private List<CoursePrerequisiteRequest> prerequisites = new ArrayList<>();

    // Optional field: if provided, must match the authenticated instructor ID
    private Integer instructorId;

    private BigDecimal price = BigDecimal.ZERO;
    private DiscountType discountType;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;
    private Integer enrollmentLimit;

    public CreateCourseRequest() {}

    public CreateCourseRequest(String title, String description) {
        this.title = title;
        this.description = description;
        this.category = "General";
    }

    public CreateCourseRequest(String title, String description, String category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public CreateCourseRequest(String title, String description, String category,
                               List<CoursePrerequisiteRequest> prerequisites,
                               List<String> learningOutcomes) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
        this.learningOutcomes = learningOutcomes != null ? learningOutcomes : new ArrayList<>();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(List<String> learningOutcomes) {
        this.learningOutcomes = learningOutcomes != null ? learningOutcomes : new ArrayList<>();
    }

    public List<CoursePrerequisiteRequest> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<CoursePrerequisiteRequest> prerequisites) {
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
        this.discountValue = discountValue;
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

    public Integer getEnrollmentLimit() {
        return enrollmentLimit;
    }

    public void setEnrollmentLimit(Integer enrollmentLimit) {
        this.enrollmentLimit = enrollmentLimit;
    }
}
