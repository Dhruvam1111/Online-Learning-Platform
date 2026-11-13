package com.olp.dto;

import com.olp.entity.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CoursePricingResponse {

    private Integer courseId;
    private String courseTitle;
    private BigDecimal basePrice = new BigDecimal("0.00");
    private BigDecimal instructorPrice = new BigDecimal("0.00");
    private DiscountType discountType;
    private BigDecimal discountValue = new BigDecimal("0.00");
    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;
    private boolean isDiscountActive;
    private BigDecimal discountAmount = new BigDecimal("0.00");
    private BigDecimal discountedPrice = new BigDecimal("0.00");
    private BigDecimal taxRate = new BigDecimal("0.18");
    private BigDecimal taxAmount = new BigDecimal("0.00");
    private BigDecimal finalPayableAmount = new BigDecimal("0.00");
    private boolean isFree = true;
    private Integer enrollmentLimit;
    private long currentEnrollmentCount = 0L;
    private boolean isEnrollmentLimitReached = false;
    private boolean hasAdminOverride = false;
    private BigDecimal adminOverridePrice;

    public CoursePricingResponse() {}

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

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getInstructorPrice() {
        return instructorPrice;
    }

    public void setInstructorPrice(BigDecimal instructorPrice) {
        this.instructorPrice = instructorPrice;
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

    public boolean isDiscountActive() {
        return isDiscountActive;
    }

    public void setDiscountActive(boolean discountActive) {
        isDiscountActive = discountActive;
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
        this.taxAmount = taxAmount != null ? taxAmount.setScale(2, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public void setFinalPayableAmount(BigDecimal finalPayableAmount) {
        this.finalPayableAmount = finalPayableAmount != null ? finalPayableAmount.setScale(2, java.math.RoundingMode.HALF_UP) : null;
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

    public long getCurrentEnrollmentCount() {
        return currentEnrollmentCount;
    }

    public void setCurrentEnrollmentCount(long currentEnrollmentCount) {
        this.currentEnrollmentCount = currentEnrollmentCount;
    }

    public boolean isEnrollmentLimitReached() {
        return isEnrollmentLimitReached;
    }

    public void setEnrollmentLimitReached(boolean enrollmentLimitReached) {
        isEnrollmentLimitReached = enrollmentLimitReached;
    }

    public boolean isHasAdminOverride() {
        return hasAdminOverride;
    }

    public void setHasAdminOverride(boolean hasAdminOverride) {
        this.hasAdminOverride = hasAdminOverride;
    }

    public BigDecimal getAdminOverridePrice() {
        return adminOverridePrice;
    }

    public void setAdminOverridePrice(BigDecimal adminOverridePrice) {
        this.adminOverridePrice = adminOverridePrice;
    }
}

