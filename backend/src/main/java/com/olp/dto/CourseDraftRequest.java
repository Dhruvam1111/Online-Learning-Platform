package com.olp.dto;

import com.olp.entity.DiscountType;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for saving or partially updating a course draft (FR-I07).
 * All fields are optional (null by default) so partial saves preserve existing course data
 * and skip course-completeness validation.
 */
public class CourseDraftRequest {

    private Integer id;
    private Integer instructorId;

    @Size(max = 200, message = "Title must be 200 characters or fewer")
    private String title;

    private String description;

    @Size(max = 100, message = "Category must be 100 characters or fewer")
    private String category;

    private List<String> learningOutcomes;
    private List<CoursePrerequisiteRequest> prerequisites;

    private BigDecimal price;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;
    private Integer enrollmentLimit;

    public CourseDraftRequest() {
    }

    public CourseDraftRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public CourseDraftRequest(String title, String description, String category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public CourseDraftRequest(
            String title,
            String description,
            String category,
            List<String> learningOutcomes,
            List<CoursePrerequisiteRequest> prerequisites) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.learningOutcomes = learningOutcomes;
        this.prerequisites = prerequisites;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Integer instructorId) {
        this.instructorId = instructorId;
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
        this.learningOutcomes = learningOutcomes;
    }

    public List<CoursePrerequisiteRequest> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<CoursePrerequisiteRequest> prerequisites) {
        this.prerequisites = prerequisites;
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

