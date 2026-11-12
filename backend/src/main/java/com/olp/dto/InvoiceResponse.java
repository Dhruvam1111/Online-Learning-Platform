package com.olp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceResponse {

    private String invoiceNumber;
    private String orderId;
    private String paymentId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer courseId;
    private String courseTitle;
    private String instructorName;
    private BigDecimal baseAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentMethod;
    private String hsnCode = "999293"; // Commercial training and education services HSN
    private String gstin = "24AAACF1234F1Z5";
    private LocalDateTime issuedAt;

    public InvoiceResponse() {}

    public InvoiceResponse(String invoiceNumber, String orderId, String paymentId,
                           Integer studentId, String studentName, String studentEmail,
                           Integer courseId, String courseTitle, String instructorName,
                           BigDecimal baseAmount, BigDecimal gstAmount, BigDecimal totalAmount,
                           String currency, String paymentMethod, LocalDateTime issuedAt) {
        this.invoiceNumber = invoiceNumber;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.instructorName = instructorName;
        this.baseAmount = baseAmount;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.issuedAt = issuedAt;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
