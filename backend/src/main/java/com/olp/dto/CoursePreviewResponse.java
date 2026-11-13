package com.olp.dto;

import com.olp.entity.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO representing the read-only student preview of a course for instructors (FR-I73).
 * Renders complete course data (including drafts and unpublished content) with enrollment
 * and progress tracking strictly disabled, including a read-only preview banner indicator.
 */
public class CoursePreviewResponse {

    // =========================================================================
    // PREVIEW BANNER & READ-ONLY ENFORCEMENT FLAGS
    // =========================================================================

    private boolean preview = true;
    private boolean readOnly = true;
    private String bannerMessage = "You are previewing this course as an instructor. Enrollments and progress tracking are disabled.";
    private boolean canEnroll = false;
    private boolean enrolled = false;
    private boolean progressTrackingEnabled = false;
    private List<String> missingContentWarnings = new ArrayList<>();

    // =========================================================================
    // COURSE CORE INFORMATION
    // =========================================================================

    private Integer id;
    private String title;
    private String description;
    private String category;
    private String level;
    private String status;
    private Integer instructorId;
    private String instructorName;
    private List<CourseInstructorResponse> instructors = new ArrayList<>();
    private List<String> learningOutcomes = new ArrayList<>();
    private List<CoursePrerequisiteResponse> prerequisites = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================================
    // COURSE STRUCTURE (MODULES, SECTIONS, LESSONS, LECTURES, QUIZZES)
    // =========================================================================

    private int totalModules;
    private int totalSections;
    private int totalLessons;
    private int totalLectures;
    private List<CourseModuleResponse> modules = new ArrayList<>();
    private List<LectureResponse> lectures = new ArrayList<>();
    private List<QuizResponse> quizzes = new ArrayList<>();

    // =========================================================================
    // PRICING BREAKDOWN (DR-PY03)
    // =========================================================================

    private CoursePricingResponse pricing;
    private BigDecimal price = new BigDecimal("0.00");
    private DiscountType discountType;
    private BigDecimal discountValue = new BigDecimal("0.00");
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;
    private BigDecimal discountAmount = new BigDecimal("0.00");
    private BigDecimal discountedPrice = new BigDecimal("0.00");
    private BigDecimal taxRate = new BigDecimal("0.18");
    private BigDecimal taxAmount = new BigDecimal("0.00");
    private BigDecimal finalPayableAmount = new BigDecimal("0.00");
    private boolean isFree = true;
    private Integer enrollmentLimit;
    private boolean isEnrollmentLimitReached = false;

    public CoursePreviewResponse() {}

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

    public boolean isPreview() {
        return preview;
    }

    public boolean getPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getBannerMessage() {
        return bannerMessage;
    }

    public void setBannerMessage(String bannerMessage) {
        this.bannerMessage = bannerMessage;
    }

    public boolean isCanEnroll() {
        return canEnroll;
    }

    public void setCanEnroll(boolean canEnroll) {
        this.canEnroll = canEnroll;
    }

    public boolean isEnrolled() {
        return enrolled;
    }

    public void setEnrolled(boolean enrolled) {
        this.enrolled = enrolled;
    }

    public boolean isProgressTrackingEnabled() {
        return progressTrackingEnabled;
    }

    public void setProgressTrackingEnabled(boolean progressTrackingEnabled) {
        this.progressTrackingEnabled = progressTrackingEnabled;
    }

    public List<String> getMissingContentWarnings() {
        return missingContentWarnings;
    }

    public void setMissingContentWarnings(List<String> missingContentWarnings) {
        this.missingContentWarnings = missingContentWarnings != null ? missingContentWarnings : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return missingContentWarnings;
    }

    public void setWarnings(List<String> warnings) {
        this.missingContentWarnings = warnings != null ? warnings : new ArrayList<>();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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

    public List<CourseInstructorResponse> getInstructors() {
        return instructors;
    }

    public void setInstructors(List<CourseInstructorResponse> instructors) {
        this.instructors = instructors != null ? instructors : new ArrayList<>();
    }

    public List<String> getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(List<String> learningOutcomes) {
        this.learningOutcomes = learningOutcomes != null ? learningOutcomes : new ArrayList<>();
    }

    public List<CoursePrerequisiteResponse> getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(List<CoursePrerequisiteResponse> prerequisites) {
        this.prerequisites = prerequisites != null ? prerequisites : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public int getTotalLectures() {
        return totalLectures;
    }

    public void setTotalLectures(int totalLectures) {
        this.totalLectures = totalLectures;
    }

    public List<CourseModuleResponse> getModules() {
        return modules;
    }

    public void setModules(List<CourseModuleResponse> modules) {
        this.modules = modules != null ? modules : new ArrayList<>();
    }

    public List<LectureResponse> getLectures() {
        return lectures;
    }

    public void setLectures(List<LectureResponse> lectures) {
        this.lectures = lectures != null ? lectures : new ArrayList<>();
    }

    public List<QuizResponse> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<QuizResponse> quizzes) {
        this.quizzes = quizzes != null ? quizzes : new ArrayList<>();
    }

    public CoursePricingResponse getPricing() {
        return pricing;
    }

    public void setPricing(CoursePricingResponse pricing) {
        this.pricing = pricing;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price != null ? price.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
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
        this.discountValue = discountValue != null ? discountValue.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
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
        this.discountAmount = discountAmount != null ? discountAmount.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice != null ? discountedPrice.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
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
        this.taxAmount = taxAmount != null ? taxAmount.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public void setFinalPayableAmount(BigDecimal finalPayableAmount) {
        this.finalPayableAmount = finalPayableAmount != null ? finalPayableAmount.setScale(2, java.math.RoundingMode.HALF_UP) : new BigDecimal("0.00");
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

