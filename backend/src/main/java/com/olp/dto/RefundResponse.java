package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private String refundId;
    private String paymentId;
    private String orderId;
    private Integer courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String status;
    private String reason;
    private boolean enrollmentRevoked;
    private LocalDateTime createdAt;

    public RefundResponse() {}

    public RefundResponse(String refundId, String paymentId, String orderId,
                          Integer courseId, String courseTitle,
                          BigDecimal amount, String status, String reason,
                          boolean enrollmentRevoked, LocalDateTime createdAt) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.enrollmentRevoked = enrollmentRevoked;
        this.createdAt = createdAt;
    }

    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public boolean isEnrollmentRevoked() { return enrollmentRevoked; }
    public void setEnrollmentRevoked(boolean enrollmentRevoked) { this.enrollmentRevoked = enrollmentRevoked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
