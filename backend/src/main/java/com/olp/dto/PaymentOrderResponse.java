package com.olp.dto;

import java.math.BigDecimal;

public class PaymentOrderResponse {

    private String settlementCurrency;
    private BigDecimal settlementAmount;
    private BigDecimal fxRoundingInr;
    public String getSettlementCurrency(){return settlementCurrency;}
    public void setSettlementCurrency(String value){settlementCurrency=value;}
    public BigDecimal getSettlementAmount(){return settlementAmount;}
    public long getSettlementAmountMinor(){return settlementAmount==null?0:settlementAmount.movePointRight(2).longValueExact();}
    public void setSettlementAmount(BigDecimal value){settlementAmount=value;}
    public BigDecimal getFxRoundingInr(){return fxRoundingInr;}
    public void setFxRoundingInr(BigDecimal value){fxRoundingInr=value;}
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String refundPolicy;
    private String refundPolicyVersion;
    private String orderId;
    private BigDecimal amount;
    private long amountInPaise;
    private String currency;
    private Integer courseId;
    private String courseTitle;
    private String keyId;
    private String studentName;
    private String studentEmail;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal walletAmountUsed = BigDecimal.ZERO;
    private Integer riskScore;
    private String couponCode;
    private String status = "created";

    public PaymentOrderResponse() {}

    public PaymentOrderResponse(String orderId, BigDecimal amount, long amountInPaise, String currency,
                                Integer courseId, String courseTitle, String keyId,
                                String studentName, String studentEmail) {
        this.orderId = orderId;
        this.amount = amount;
        this.amountInPaise = amountInPaise;
        this.currency = currency;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.keyId = keyId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    public BigDecimal getTaxRate(){return taxRate;}
    public void setTaxRate(BigDecimal value){taxRate=value;}
    public BigDecimal getTaxAmount(){return taxAmount;}
    public void setTaxAmount(BigDecimal value){taxAmount=value;}
    public BigDecimal getTotalAmount(){return totalAmount;}
    public void setTotalAmount(BigDecimal value){totalAmount=value;}
    public String getRefundPolicy(){return refundPolicy;}
    public void setRefundPolicy(String value){refundPolicy=value;}
    public String getRefundPolicyVersion(){return refundPolicyVersion;}
    public void setRefundPolicyVersion(String value){refundPolicyVersion=value;}
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public long getAmountInPaise() { return amountInPaise; }
    public void setAmountInPaise(long amountInPaise) { this.amountInPaise = amountInPaise; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getWalletAmountUsed() { return walletAmountUsed; }
    public void setWalletAmountUsed(BigDecimal walletAmountUsed) { this.walletAmountUsed = walletAmountUsed; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
