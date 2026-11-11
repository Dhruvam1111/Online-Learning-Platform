package com.olp.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreatePaymentOrderRequest {

    @NotNull(message = "Course ID is required")
    private Integer courseId;
    private String settlementCurrency = "INR";
    public String getSettlementCurrency(){return settlementCurrency;}
    public void setSettlementCurrency(String value){settlementCurrency=value;}
    private String policyAcceptedVersion;
    public String getPolicyAcceptedVersion(){return policyAcceptedVersion;}
    public void setPolicyAcceptedVersion(String value){policyAcceptedVersion=value;}

    // Prices and currency in HTTP checkout are determined by the server.
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private BigDecimal amount;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String currency = "INR";

    private String couponCode;

    private Boolean useWallet = false;

    private String paymentMethod;

    public CreatePaymentOrderRequest() {}

    public CreatePaymentOrderRequest(Integer courseId, BigDecimal amount, String currency) {
        this.courseId = courseId;
        this.amount = amount;
        this.currency = currency;
    }

    public CreatePaymentOrderRequest(Integer courseId, BigDecimal amount, String currency, String couponCode, Boolean useWallet) {
        this.courseId = courseId;
        this.amount = amount;
        this.currency = currency;
        this.couponCode = couponCode;
        this.useWallet = useWallet;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("courseId")
    public void setCourseIdFromJson(Object val) {
        if (val instanceof Number n) {
            this.courseId = n.intValue();
        } else if (val instanceof String s) {
            try {
                this.courseId = Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                if (s.contains("design-system")) this.courseId = 7;
                else if (s.contains("ui-ux") || s.contains("advance")) this.courseId = 6;
                else if (s.contains("webflow")) this.courseId = 8;
                else if (s.contains("fullstack") || s.contains("modern")) this.courseId = 9;
                else this.courseId = 1;
            }
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Boolean getUseWallet() {
        return useWallet;
    }

    public void setUseWallet(Boolean useWallet) {
        this.useWallet = useWallet;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
