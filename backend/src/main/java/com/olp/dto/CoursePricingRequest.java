package com.olp.dto;

import com.olp.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CoursePricingRequest {

    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal price = BigDecimal.ZERO;

    private DiscountType discountType;

    @DecimalMin(value = "0.00", message = "Discount value cannot be negative")
    private BigDecimal discountValue = BigDecimal.ZERO;

    private LocalDateTime discountStartDate;
    private LocalDateTime discountEndDate;

    @Min(value = 1, message = "Enrollment limit must be at least 1")
    private Integer enrollmentLimit;

    public CoursePricingRequest() {}

    public CoursePricingRequest(BigDecimal price, DiscountType discountType, BigDecimal discountValue,
                                LocalDateTime discountStartDate, LocalDateTime discountEndDate,
                                Integer enrollmentLimit) {
        this.price = price != null ? price : BigDecimal.ZERO;
        this.discountType = discountType;
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
        this.discountStartDate = discountStartDate;
        this.discountEndDate = discountEndDate;
        this.enrollmentLimit = enrollmentLimit;
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

